# TROUBLESHOOTING: CloudWatch Observability (Container Insights) 및 모니터링 아키텍처 결정

- 작성일: 2026-07-24
- 대상 클러스터: `smtm-eks` (Region: `ap-northeast-2`)
- 관련 Add-on: `amazon-cloudwatch-observability` (v6.3.0-eksbuild.1)
- 관련 가이드 절: `SMTM_BROWNFIELD_FULL_GUIDE.md` 5.2 Container Insights 화면 확인 ~ 5.5 Dashboard 생성

---

## 1. 배경

가이드 5.2(Container Insights 화면 확인) 단계에서 AWS Console → CloudWatch → Insights → Container Insights 화면에 Cluster/Node/Pod 지표가 전혀 표시되지 않는 문제 발생. 원인 조사 과정에서 총 3단계의 서로 다른 문제가 순차적으로 발견되었고, 최종적으로는 CloudWatch 단독으로는 해결이 안 되는 지점까지 확인한 뒤 모니터링 아키텍처 자체를 조정하기로 결정함.

---

## 2. 트러블슈팅 타임라인

### 2.1 1차 원인 — Pod Identity 대신 노드 IAM Role로 인증 시도 (해결됨)

**증상**

- `aws cloudwatch list-metrics --namespace ContainerInsights`가 빈 배열 반환
- CloudWatch Logs 로그 그룹(`/aws/containerinsights/smtm-eks/*`)은 일부 데이터가 쌓이지만 간헐적

**진단**

- `amazon-cloudwatch` 네임스페이스 Pod(`cloudwatch-agent`, `cloudwatch-agent-cluster-scraper`)는 모두 `Running`, add-on 상태도 `ACTIVE`
- Pod Identity association도 정상 등록되어 있었음
- 그러나 로그에서 다음 에러 확인:
  ```
  User: arn:aws:sts::061039804626:assumed-role/smtm-eks-node-role/i-...
  is not authorized to perform: cloudwatch:PutMetricData / logs:PutLogEvents
  ```
  → Pod Identity Role(`smtm-observability-role`)이 아니라 **EC2 노드 인스턴스 Role**로 인증 시도, 노드 Role에는 CloudWatch 쓰기 권한이 없어 403 발생

**조치**

```bash
kubectl -n amazon-cloudwatch rollout restart daemonset/cloudwatch-agent
kubectl -n amazon-cloudwatch rollout restart deployment/cloudwatch-agent-cluster-scraper
```

**결과**: 재기동 직후 PermissionDenied/AccessDenied 에러 소멸. Pod가 Pod Identity 연결이 적용되기 전에 떠 있던 상태였던 것으로 추정.

---

### 2.2 2차 원인 — `eks-pod-identity-agent` 내부 자격증명 캐시 만료 (해결됨)

**증상**

- 1차 조치 직후에는 정상이었으나, `eks-pod-identity-agent` 자체 로그에서 다음 반복 확인:
  ```
  ExpiredTokenException: The token included in the request is expired:
  current date/time X must be before the expiration date/time Y
  ```
- vpc-cni, cluster-autoscaler, external-secrets, aws-load-balancer-controller 등 **클러스터 내 거의 모든 Pod Identity 연결에서 동일 증상** 확인
- "현재 시각 vs 토큰 만료 시각" 격차가 시간이 지날수록 점점 벌어짐(1h23m → 2h23m → 2h38m)

**배제한 원인**

- 노드 시계 오차: SSM으로 3개 노드 중 2개 직접 접속하여 `timedatectl`, `chronyc tracking` 확인 → 완전 동기화 상태(오차 마이크로초 단위), 원인 아님
- kubelet 토큰 로테이션 실패: `journalctl -u kubelet`에 token/rotate/expir 관련 로그 0건 → kubelet은 정상 갱신 중, 원인 아님

**결론**: `eks-pod-identity-agent`가 워크로드 Pod의 최신 ServiceAccount 토큰을 재조회하지 않고, 최초 자격증명 발급 시점의 캐시를 계속 재사용하며 갱신(renewal-thread)을 시도하다 실패하는 것으로 추정.

**조치**

```bash
kubectl -n kube-system rollout restart daemonset/eks-pod-identity-agent
```

add-on 버전은 이미 최신(`v1.3.10-eksbuild.3`)으로 확인되어 버전 업그레이드는 불필요.

**검증**: 재기동 직후 및 토큰 TTL 경과(약 45분~1시간) 후 재확인 모두 `ExpiredToken`/`non recoverable` 에러 재발 없음 → **재발 방지 확인 완료**

---

### 2.3 3차 원인 — Container Insights(cadvisor 기반) 노드/파드 리소스 지표 미수집 (미해결, 보류)

**증상**

- 위 두 인증 문제를 모두 해결한 뒤에도 `node_cpu_utilization`, `node_memory_utilization`, `cluster_node_count` 등 노드/파드 리소스 지표가 계정 전체(11,296개 메트릭 전수 조사)에서 단 하나도 발견되지 않음
- 계정에는 `AWS/EKS`(컨트롤플레인 apiserver/etcd/scheduler, 28개), `ApplicationSignals`(APM), 일반 로그 그룹 지표만 존재
- `cloudwatch-agent` daemonset 로그에서 `kubelet`, `cadvisor`, `10250`, `scrape` 관련 언급이 전혀 없음 → 에러로 실패하는 게 아니라 **cadvisor 수집 시도 자체가 발생하지 않는 것으로 추정**
- 별도로 확인된 이슈: `Partial success response - Summary datapoints are not supported` 경고가 최근 로그의 절반가량(148/316줄) 차지. CloudWatch OTLP 수신 엔드포인트가 OTel "Summary" 타입 데이터포인트를 지원하지 않아 지속적으로 드롭됨(주로 percentile 계열 지표로 추정, add-on 구조적 한계)
- 반복되는 `TLS handshake error: client sent an HTTP request to an HTTPS server` 로그도 지속 확인(원인 미상, 내부 컴포넌트 간 mTLS 설정 이슈로 추정)

**시도한 조치**

1. `aws eks describe-addon ... --query 'addon.configurationValues'` 확인
   ```json
   {"containerInsights": {"enabled": false}, "otelContainerInsights": {"enabled": true}}
   ```
   → 설정값 자체는 정상(OTel 기반 Container Insights 활성화 상태)
2. AWS Console에서 add-on 설정을 강제로 변경(`otelContainerInsights.enabled`를 `false` → `true`로 2회 토글)하여 재구성(reconcile) 강제 유도
3. 토글 후 `amazon-cloudwatch` 네임스페이스 전체 Pod가 재생성됨(AGE 초기화, controller-manager 포함) 확인 → 재구성 자체는 정상 수행됨
4. 재구성 후 재확인 → **동일 증상 지속**(cadvisor 메트릭 0개, 동일한 로그 패턴 반복)

**결론**

- 인증(Pod Identity) 문제는 완전히 해결되었으나, **cadvisor 기반 노드/파드 리소스 메트릭 수집 파이프라인 자체가 동작하지 않는 상태**는 add-on 재구성으로도 해결되지 않음
- kubectl/AWS CLI/Console 레벨에서 확인 가능한 범위를 벗어난 것으로 판단(OTel Collector 내부 파이프라인 배선 문제 또는 add-on v6.3.0-eksbuild.1의 알려진 제약/버그 가능성)
- **현재 상태: 보류(Known Issue)**. 추가 조치가 필요하면 AWS Support 문의 대상으로 남겨둠

---

## 3. 프로젝트 방향 변경 결정

위 3.3 이슈로 인해 CloudWatch Container Insights만으로는 노드/파드 단위 리소스 모니터링을 완성할 수 없다고 판단, 모니터링 아키텍처를 다음과 같이 조정함.

| 영역 | 기존 계획 | 변경 후 |
|---|---|---|
| 알림(Notification) | CloudWatch Alarm → SNS | **변경 없음** — CloudWatch Alarm → SNS(Email + Slack) 그대로 유지 |
| 대시보드(시각화) | CloudWatch Dashboard(5.5) 전체 위젯 구성 | **CloudWatch Dashboard 구축 중단**(부분 완성 상태로 보류) |
| 노드/파드 리소스 모니터링 | CloudWatch Container Insights | **kube-prometheus-stack(Prometheus + Grafana + node-exporter)로 대체** |
| 애플리케이션 지표 | (미정) | Backend에 Actuator/Micrometer-Prometheus 추가 → ServiceMonitor로 Prometheus 연동 |

**근거**

- CloudWatch Alarm은 Dashboard 존재 여부와 무관하게 독립적으로 동작하므로, Dashboard를 완성하지 않아도 알림 파이프라인에는 영향 없음
- kube-prometheus-stack에 포함된 `prometheus-node-exporter`가 CloudWatch Container Insights가 주려던 것과 동일한 노드 CPU/메모리 지표를 Grafana를 통해 대체 제공하므로, 3.3의 미해결 이슈가 실질적인 모니터링 공백으로 이어지지 않음
- 알림(SNS)과 시각화(Grafana)는 서로 다른 시스템으로 분리 유지. `kube-prometheus-stack`의 Alertmanager는 배포는 되지만 receiver(Slack/Email 등)를 별도로 구성하지 않아 알림 경로로 사용하지 않음(SNS와의 중복 알림 방지)

---

## 4. 남은 작업 순서

1. **6. Backend에 Prometheus 지표 추가** — `build.gradle`(Actuator/Micrometer), `application.yml`(endpoint 노출 제한), `SecurityConfig.java`(GET permitAll), 이미지 재빌드/ECR Push/Helm 재배포
2. **5.6 CloudWatch Alarm 생성** — SNS Topic(`smtm-ops-alerts`) 기준 6개 Alarm 구성(Dashboard 없이도 독립 진행 가능)
3. **7. Prometheus/Grafana 구성(옵션 A, 자체 호스팅)** — `kube-prometheus-stack` 설치, 7.4 ServiceMonitor 적용까지 완료해야 Backend 지표가 Grafana에 표시됨

---

## 5. 참고 — 조사에 사용한 주요 명령어

```bash
# add-on 상태/설정 확인
aws eks describe-addon --cluster-name smtm-eks --addon-name amazon-cloudwatch-observability \
  --region ap-northeast-2 --query 'addon.[status,addonVersion,health.issues]' --output json
aws eks describe-addon --cluster-name smtm-eks --addon-name amazon-cloudwatch-observability \
  --region ap-northeast-2 --query 'addon.configurationValues'

# Pod Identity 연결 확인
aws eks list-pod-identity-associations --cluster-name smtm-eks --region ap-northeast-2

# 계정 전체 메트릭 중 Container Insights 스타일 지표 존재 여부 확인
aws cloudwatch list-metrics --region ap-northeast-2 --output json \
  | python3 -c "
import json,sys
d = json.load(sys.stdin)
names = sorted(set(m['MetricName'] for m in d['Metrics']))
hits = [n for n in names if any(k in n.lower() for k in ['node_cpu','node_memory','pod_cpu','pod_memory','node_filesystem','cluster_node_count'])]
print(len(hits), 'container insights style metrics found')
"

# 로그 확인
kubectl -n amazon-cloudwatch logs -l app.kubernetes.io/name=cloudwatch-agent --tail=200 --since=30m
kubectl -n kube-system logs -l app.kubernetes.io/name=eks-pod-identity-agent --tail=200 --since=1h | grep -i "ExpiredToken\|non recoverable"

# 노드 시계 동기화 확인 (SSM)
aws ssm start-session --target <instance-id> --region ap-northeast-2
# 세션 내부: timedatectl / chronyc tracking / journalctl -u kubelet --since "2 hours ago"
```
