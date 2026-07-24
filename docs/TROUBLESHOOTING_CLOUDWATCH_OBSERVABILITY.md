# TROUBLESHOOTING: CloudWatch Observability (Container Insights) 및 모니터링 아키텍처 결정

- 작성일: 2026-07-24
- 대상 클러스터: `smtm-eks` (Region: `ap-northeast-2`)
- 관련 Add-on: `amazon-cloudwatch-observability` (v6.3.0-eksbuild.1)
- 다루는 범위: Container Insights 지표 확인부터 CloudWatch Dashboard 구성까지

---

## 1. 배경

Container Insights 화면 확인 단계에서 AWS Console → CloudWatch → Insights → Container Insights 화면에 Cluster/Node/Pod 지표가 전혀 표시되지 않는 문제 발생. 원인 조사 과정에서 여러 단계의 서로 다른 문제가 순차적으로 발견되었고, **최종적으로 근본 원인이 AWS 서비스 자체의 리전 제약(Preview 미지원)이라는 것까지 확인**했다.

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

### 2.3 3차 원인 — Container Insights(cadvisor 기반) 노드/파드 리소스 지표 미수집 (근본 원인 확인 완료)

#### 2.3.1 초기 조사 (인증/설정 관점)

- 위 두 인증 문제를 모두 해결한 뒤에도 `node_cpu_utilization`, `node_memory_utilization`, `cluster_node_count` 등 노드/파드 리소스 지표가 계정 전체(11,296개 메트릭 전수 조사)에서 단 하나도 발견되지 않음
- 계정에는 `AWS/EKS`(컨트롤플레인 apiserver/etcd/scheduler, 28개), `ApplicationSignals`(APM), 일반 로그 그룹 지표만 존재
- `Partial success response - Summary datapoints are not supported` 경고가 로그의 상당 비중 차지. CloudWatch OTLP 수신 엔드포인트가 OTel "Summary" 타입 데이터포인트를 지원하지 않아 지속적으로 드롭됨
- 반복되는 `TLS handshake error: client sent an HTTP request to an HTTPS server` 로그도 지속 확인
- `aws eks describe-addon ... --query 'addon.configurationValues'` 확인 결과 `{"containerInsights": {"enabled": false}, "otelContainerInsights": {"enabled": true}}` → 설정값 자체는 정상
- AWS Console에서 add-on 설정을 강제로 변경(`otelContainerInsights.enabled` off→on 2회 토글)하여 재구성 강제 유도 → Pod 전체 재생성 확인되었으나 **동일 증상 지속**

#### 2.3.2 심층 재진단 (진단 스크립트 기반)

전용 진단 스크립트(`cw_observability_diagnose.sh`)로 아래를 교차 확인:

| 확인 항목 | 결과 |
|---|---|
| kubelet cadvisor 엔드포인트 직접 확인 (`kubectl get --raw /api/v1/nodes/<node>/proxy/metrics/cadvisor`) | **정상**. 3개 노드 전부 실제 cadvisor 지표 반환 |
| `AmazonCloudWatchAgent` CR의 OTel Collector 설정 | **정상**. `kubeletstats/cw_k8s_ci_v0`, `prometheus/cw_k8s_ci_v0_cadvisor` 리시버가 `otlphttp/cw_k8s_ci_v0_metrics_dest`(`https://monitoring.ap-northeast-2.amazonaws.com`) 익스포터에 정확히 배선되어 있음 |
| Export 로그 (2시간 집계) | `Partial success response` 719건. AWS 쪽에서 응답은 오고 있음 |
| `ClusterName=smtm-eks` Dimension 기준 전수 조회 | **28개만 존재, 전부 컨트롤플레인(apiserver/etcd/scheduler) 지표.** Node/Pod 관련 지표 0개 |
| `CWAgent` 네임스페이스 확인 | 우리 클러스터와 무관한 **다른 Ubuntu 계열 EC2 인스턴스**들의 지표였음 (오탐, 배제) |
| IAM Role(`smtm-observability-role`) 정책 확인 | `CloudWatchAgentServerPolicy` 정상 부착, AWS 공식 문서 요구사항과 일치. 403이 아닌 "Partial success"라는 점에서 인증 문제 아님 재확인 |
| AWS 공식 트러블슈팅 문서(Unauthorized panic, No pod metric collected 등 알려진 이슈) 대조 | 로그에 관련 문자열 0건, 전부 해당 없음 |

**결론**: kubelet, RBAC, add-on 설정, IAM 권한, 네트워크(export 자체는 성공)까지 전부 정상인데도 최종적으로 지표가 CloudWatch에 안착하지 않는 상태. CLI/콘솔로 확인 가능한 범위를 완전히 벗어남.

#### 2.3.3 Classic(EMF 기반) Container Insights 재시도

OTel 방식과 완전히 다른 코드 경로인 classic 방식(`containerInsights.enabled`)을 시험적으로 활성화:

```bash
aws eks update-addon --cluster-name smtm-eks --addon-name amazon-cloudwatch-observability \
  --region ap-northeast-2 --configuration-values '{"containerInsights":{"enabled":true}}' \
  --resolve-conflicts OVERWRITE
```

- `fluent-bit-*` DaemonSet Pod 3개 신규 활성화, `/aws/containerinsights/smtm-eks/performance` 로그 그룹 신규 생성 확인
- **실제 버그 발견**: fluent-bit 로그에서
  ```
  [error] [tls] error: error:0A000086:SSL routines::certificate verify failed
  [error] [filter:kubernetes:kubernetes.1] [kubernetes] no upstream connections available to cloudwatch-agent.amazon-cloudwatch:4311
  ```
  → 내부 서비스(`cloudwatch-agent:4311`)의 TLS 인증서가 `containerInsights.enabled` 토글 시점에 막 재발급(`notBefore`가 업데이트 명령 시각과 거의 일치)되었는데, fluent-bit이 이전 신뢰 정보로 기동되어 TLS 검증 실패
- `kubectl rollout restart daemonset/fluent-bit`로 재기동 → 해당 에러는 소멸(재현 안 됨), 실제로 원인을 찾아 해결한 사례
- 재기동 직후 `performance` 로그 그룹 `storedBytes`는 계속 0으로 관찰되어 이 시점에는 "classic도 데이터 미적재"로 잠정 결론 → **이후 2.3.5에서 이 결론이 뒤집힘 (red herring이었음)**

#### 2.3.4 근본 원인 확정 — OTel Container Insights는 서울 리전 미지원 (Public Preview)

AWS 공식 발표를 확인한 결과, 지금까지의 모든 증상을 설명하는 근본 원인을 확인했다.

> **"Amazon CloudWatch launches OTel Container Insights for Amazon EKS (Preview)"** (2026-04-02)
> "Container Insights with OpenTelemetry metrics is available in public preview in **US East (N. Virginia), US West (Oregon), Asia Pacific (Sydney), Asia Pacific (Singapore), and Europe (Ireland)**."

**서울 리전(`ap-northeast-2`)은 이 프리뷰 지원 리전 목록에 없음.** 지금까지 계속 다뤘던 `otelContainerInsights.enabled` 기능 자체가 이 Preview 기능이며, 서울 리전에는 아직 백엔드 처리 파이프라인이 배포되어 있지 않은 것으로 판단된다.

이는 지금까지의 모든 관찰과 정합적이다.
- 클라이언트 측(kubelet, RBAC, Pod Identity, add-on 설정, 네트워크 egress)은 전부 정상이었던 이유 → 문제가 클러스터가 아니라 리전 백엔드에 있었기 때문
- Export가 "Partial success"로 응답은 받았지만 실제 지표가 하나도 안 쌓인 이유 → 리전의 기본 API 엔드포인트는 응답하나, Preview 전용 처리 로직이 서울 리전에 없어 실제 저장/노출이 안 되는 것으로 추정
- 커뮤니티(GitHub Issues, 포럼)에서 동일 증상 사례를 거의 찾을 수 없었던 이유 → 지원 리전에서는 정상 동작하기 때문에 애초에 이 문제로 고생하는 사례 자체가 적음

~~Classic 방식(2.3.3)이 함께 안 된 것은, 두 방식(OTel/classic)을 동시에 활성화한 상태에서 생긴 부작용일 가능성이 높다~~ → **잘못 추정함 (2.3.5 참조).** Classic이 그 시점에 0으로 보였던 진짜 이유는 fluent-bit TLS 캐시 버그가 매번 add-on 설정을 바꿀 때마다 재발하는데 그때는 재기동 확인이 불충분했던 것과, `performance` 로그 그룹의 `storedBytes` 집계 자체가 실시간이 아니라 지연되는 게이지 값이라 너무 이른 시점에 확인해 0으로 보였던 것 두 가지가 겹친 결과였다 (자세한 내용은 2.3.5 참조).

**참고 문서**
- [Amazon CloudWatch launches OTel Container Insights for Amazon EKS (Preview)](https://aws.amazon.com/about-aws/whats-new/2026/04/cloudwatch-otel-container-insights-eks)
- [Container Insights with OpenTelemetry metrics for Amazon EKS](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/container-insights-otel-metrics.html)
- [Container Insights 지표 보기 (한국어)](https://docs.aws.amazon.com/ko_kr/AmazonCloudWatch/latest/monitoring/Container-Insights-view-metrics.html)
- [Container Insights 문제 해결 (한국어, 공식 알려진 이슈 목록)](https://docs.aws.amazon.com/ko_kr/AmazonCloudWatch/latest/monitoring/ContainerInsights-troubleshooting.html)
- [How to configure AWS Observability addon to record pod metrics but not API metrics · Issue #322](https://github.com/aws/amazon-cloudwatch-agent-operator/issues/322)
- [Install the CloudWatch agent with the Amazon CloudWatch Observability EKS add-on](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Observability-EKS-addon.html)

**조치(당초)**: `containerInsights.enabled`는 원상복구(`false`) 완료. 서울 리전이 Preview 지원 리전에 포함되기 전까지는 재시도하지 않음. → **이후 2.3.5에서 classic을 다시 켜서 재검증, 현재는 `containerInsights.enabled: true / otelContainerInsights.enabled: false`로 운영 중.**

#### 2.3.5 최종 반전 — Classic Container Insights 정상 동작 확인 (2026-07-24, 재검증)

2.3.4까지는 "OTel은 리전 미지원, classic도 데이터 미적재"로 결론 내렸으나, `containerInsights.enabled:true` / `otelContainerInsights.enabled:false`로 재구성 후 **fluent-bit 데몬셋을 명시적으로 재기동**하고 나서 재검증한 결과 classic이 정상 동작함을 확인했다.

**재현 및 검증 절차**

1. Add-on 재구성:
   ```bash
   aws eks update-addon --cluster-name smtm-eks --addon-name amazon-cloudwatch-observability \
     --region ap-northeast-2 \
     --configuration-values '{"containerInsights":{"enabled":true},"otelContainerInsights":{"enabled":false}}' \
     --resolve-conflicts OVERWRITE
   ```
2. `kubectl get pods -n amazon-cloudwatch -o wide`로 확인한 결과 **cloudwatch-agent 파드만 재생성되고 fluent-bit 파드는 이전 그대로(AGE 불일치)** → 2.3.3과 동일한 TLS 인증서 불일치 재발 확인 (`certificate verify failed`, `no upstream connections available to cloudwatch-agent.amazon-cloudwatch:4311`)
3. `kubectl -n amazon-cloudwatch rollout restart daemonset/fluent-bit` 실행 후 `rollout status`로 완료 확인 → fluent-bit 파드 전체 AGE 1분 이내로 재생성됨
4. 5분 대기 후 `aws logs describe-log-groups --log-group-name-prefix "/aws/containerinsights/smtm-eks"` 로 **전체 로그 그룹**을 확인:

   | 로그 그룹 | storedBytes | 용도(AWS 공식 문서 기준) |
   |---|---|---|
   | `/aws/containerinsights/smtm-eks/application` | 142,599,237 | 컨테이너 stdout/stderr 로그 |
   | `/aws/containerinsights/smtm-eks/dataplane` | 2,982,992 | kubelet.service/kube-proxy.service/docker.service 등 노드 시스템 서비스 로그 (지표 아님) |
   | `/aws/containerinsights/smtm-eks/host` | 2,841,657 | 노드/호스트 일반 로그 (지표 아님) |
   | `/aws/containerinsights/smtm-eks/performance` | 0 | **Node/Pod/Container EMF 성능 지표 (실제로 여기가 맞는 위치)** |

   > **정정(2026-07-24)**: 최초 이 표를 보고 "`performance`가 아니라 `dataplane`/`host`에 실제 데이터가 쌓인다"고 판단했으나 이는 오판이었다. AWS 공식 문서([Container Insights performance log events](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Container-Insights-reference-performance-logs-EKS.html))에 따르면 `dataplane`/`host`는 시스템 서비스/호스트 로그일 뿐이고, Node/Pod/Container EMF 성능 지표는 원래부터 `performance` 로그 그룹이 맞는 위치다. 즉 로그 그룹 이름을 잘못 짚은 게 아니었다.
   >
   > 실제 원인은 **`storedBytes`가 실시간 집계가 아니라 지연되는 메터링 게이지**라는 점이다. 재기동 후 5분 만에 확인했을 때는 이 카운터가 아직 갱신되지 않아 0으로 보였을 뿐, 그 시점에도 `list-metrics`/`get-metric-statistics`로 조회하면 실제 지표는 이미 정상적으로 쌓이고 있었다(아래 5, 6번 확인 참조). `dataplane`/`host`에 잡힌 바이트는 원래 목적대로 시스템/호스트 로그가 정상 수집된 것일 뿐, Container Insights 지표와는 무관하다. OTel 방식은 애초에 이 로그 그룹들 자체를 쓰지 않는다(OTLP로 CloudWatch 지표 엔드포인트에 직접 전송하므로 `containerInsights`/`otelContainerInsights` 어느 쪽 이름 규칙과도 무관한 순수 메터링 지연 이슈였다).

5. `aws cloudwatch list-metrics --namespace ContainerInsights --dimensions Name=ClusterName,Value=smtm-eks`로 지표 카탈로그 확인 → 이전(28개, 컨트롤플레인 전용)과 달리 **`node_cpu_utilization`, `node_memory_utilization`, `pod_cpu_utilization`, `pod_memory_utilization`, `pod_number_of_container_restarts`, `replicas_desired`, `replicas_ready` 등 90여 종, 3,657개 항목**이 `kube-system`/`amazon-cloudwatch`/`argocd`/`smtm` 등 여러 네임스페이스에 걸쳐 존재
6. `aws cloudwatch get-metric-statistics`로 실제 데이터포인트 존재 여부까지 직접 검증(카탈로그 등재만으로는 실시간 수집 여부를 보장하지 않으므로):
   - `node_cpu_utilization` (`ClusterName=smtm-eks`): 1분 간격으로 20분간 끊김 없이 수집 확인 (평균 5% 내외)
   - `replicas_desired` (`ClusterName=smtm-eks, Namespace=smtm, PodName=smtm-backend`): 1분 간격 `2.0` 지속 확인
   - `replicas_ready` (동일 디멘션): 1분 간격 `2.0` 지속 확인

**결론**: Classic(Enhanced) Container Insights는 정상 동작하며, Node/Pod/Container 리소스 지표는 물론 HPA 시각화에 필요한 `replicas_desired`/`replicas_ready`까지 실시간으로 수집되고 있다. 2.3.3~2.3.4에서 "classic도 실패"로 판단했던 것은 (1) fluent-bit TLS 캐시 버그가 add-on 설정 변경 시마다 재발하는데 매번 재기동 확인을 안 했고, (2) 정상적으로 데이터가 쌓이는 `performance` 로그 그룹을, storedBytes 메터링 지연 때문에 너무 이른 시점에 확인해서 0으로 오판했기 때문이었다(로그 그룹 자체를 잘못 짚은 게 아니었음). OTel 방식이 리전 미지원이라는 2.3.4의 결론은 변함없지만, **classic 방식은 정상 동작 가능**하다는 것이 최종 확정 사실이다. 이후 콘솔의 "찾아보기" 화면에서 smtm-backend 파드 기준 지표 33개(CPU/메모리/네트워크/상태/재시작/HPA 전부 포함)가 전량 수집되고 있음을 추가로 재확인했다.

---

## 3. 프로젝트 방향 변경 결정

> **2026-07-24 업데이트**: 2.3.5에서 classic Container Insights가 fluent-bit 재기동 이후 정상 동작함이 확인되어(Node/Pod 지표 + `replicas_desired`/`replicas_ready` HPA 지표 포함), 아래 표의 "변경 후" 결정 중 노드/파드 모니터링·HPA 시각화 항목은 재검토를 거쳤다. 최초 이 표를 작성할 당시엔 "OTel 미지원 + classic도 실패"로 알고 있었으나, 실제로는 classic만으로 필요한 지표를 전부 확보할 수 있는 상태였다. 아래는 그 시점 기준 결정 내역이며, **최종 결정은 표 하단 "최종 방향"을 따른다.**

| 영역 | 기존 계획 | 변경 후(당초 결정, 2.3.4 기준) |
|---|---|---|
| 알림(Notification) | CloudWatch Alarm → SNS | **변경 없음** — CloudWatch Alarm → SNS(Email + Slack) 그대로 유지 |
| 대시보드(시각화) | CloudWatch Dashboard 전체 위젯 구성 | ~~CloudWatch Dashboard 구축 중단(부분 완성 상태로 보류)~~ → **정정: classic 정상 동작 확인(2.3.5)으로 구축 재개 가능, 아래 "최종 방향" 참조** |
| 노드/파드 리소스 모니터링 | CloudWatch Container Insights | ~~kube-prometheus-stack(Prometheus + Grafana + node-exporter)로 대체 (리전 미지원으로 확정)~~ → **정정: "대체"가 아니라 "병행/확장"으로 변경, 아래 "최종 방향" 참조** |
| 애플리케이션 지표 | (미정) | Backend에 Actuator/Micrometer-Prometheus 추가 → ServiceMonitor로 Prometheus 연동 (완료) |
| HPA 동작 시각화 | (미정, Container Insights엔 애초에 HPA 지표 없음) | `kube-state-metrics`(`kube_horizontalpodautoscaler_status_*`)로 확보 |

**당초 근거 (2.3.4 시점)**

- CloudWatch Alarm은 Dashboard 존재 여부와 무관하게 독립적으로 동작하므로, Dashboard를 완성하지 않아도 알림 파이프라인에는 영향 없음
- 근본 원인이 "리전 Preview 미지원"으로 확정되어, 추가 설정 변경으로는 해결 불가능함이 명확해짐
- kube-prometheus-stack의 `prometheus-node-exporter` + kubelet cadvisor 스크레이핑 + `kube-state-metrics`가 CloudWatch Container Insights가 주려던 것(Node/Pod CPU·Memory)은 물론, Container Insights에는 애초에 없는 것으로 알았던 HPA 동작 지표까지 대체 제공 가능 (→ 이후 2.3.5에서 Container Insights에도 HPA 지표가 있었음이 밝혀지면서, 이 "대체" 논리는 최종 방향에서 "병행" 논리로 바뀜)
- 알림(SNS)과 시각화(Grafana)는 서로 다른 시스템으로 분리 유지. `kube-prometheus-stack`의 Alertmanager는 배포는 되지만 receiver(Slack/Email 등)를 별도로 구성하지 않아 알림 경로로 사용하지 않음(SNS와의 중복 알림 방지)

**재검토 (2.3.5 반영)**

- classic Container Insights가 `replicas_desired`/`replicas_ready`까지 포함해 정상 동작하므로, "Container Insights에는 HPA 지표가 없다"는 전제가 틀렸음이 확인됨
- 즉 CloudWatch Dashboard만으로 Node/Pod/HPA 시각화가 전부 가능해졌고, kube-prometheus-stack 도입은 더 이상 "지표 확보를 위한 필수 대체재"가 아니게 됨
- 다만 classic도 add-on 설정을 바꿀 때마다 fluent-bit TLS 캐시 버그가 재발하는 구조적 결함이 있어(2.3.3, 2.3.5), 운영 중 add-on을 재구성할 일이 생기면 매번 fluent-bit 수동 재기동이 필요함

**최종 방향**

- **CloudWatch Dashboard 구축을 재개한다.** 중단(보류) 상태를 해제하고, 확보된 지표(`node_cpu_utilization`, `node_memory_utilization`, `replicas_desired`, `replicas_ready` 등)로 위젯 구성을 이어간다.
- **kube-prometheus-stack은 CloudWatch Container Insights를 "대체"하는 게 아니라 "확장"하는 것으로 정정한다.** classic이 정상 동작하는 이상 대체할 필요 자체가 없고, kube-prometheus-stack은 CloudWatch가 주지 않는 것(PromQL 세부 쿼리, Grafana 커스텀 대시보드, 애플리케이션 지표와의 통합 시각화 등)을 보강하는 용도로 추가 구축한다.

CloudWatch Container Insights(classic)와 kube-prometheus-stack을 **둘 다 구축**하는 것으로 최종 결정함. 각 도구는 다음과 같이 용도를 분리하여 사용할 예정임

- CloudWatch Dashboard(classic Container Insights 기반) + CloudWatch Alarm → SNS(Email/Slack): 운영 알림·헬스체크용
- kube-prometheus-stack(Prometheus + Grafana): 데모·시각화, PromQL 기반 세부 분석용
- 두 시스템은 같은 kubelet/cadvisor 데이터를 각자 독립적으로 수집하므로 리소스 경쟁이나 충돌 없음
- classic을 유지하는 한 add-on 설정 변경 시 fluent-bit 재기동은 항상 챙길 것

---

## 4. 남은 작업 순서

모니터링 스택은 CloudWatch(classic Container Insights)와 kube-prometheus-stack **둘 다 구축**하는 것으로 최종 결정했다(3장 "최종 방향" 참조). 남은 작업 순서:

1. **CloudWatch Dashboard 재개** — 중단했던 Dashboard 구축을 `node_cpu_utilization`/`node_memory_utilization`/`replicas_desired`/`replicas_ready` 위젯으로 재개
2. **kube-prometheus-stack 설치** — Helm 설치(현재 설치됨), ServiceMonitor 적용까지 완료해야 Backend 지표가 Grafana에 표시됨
3. **CloudWatch Alarm 생성** — SNS Topic(`smtm-ops-alerts`) 기준 알람 구성(Dashboard/Prometheus 설치와 무관하게 독립 진행 가능)
4. **Backend에 Prometheus 지표 추가** — 완료 (`build.gradle`, `application.yml`, `SecurityConfig.java` 배포 완료, 이미지 최종 버전 ECR 푸시 및 Helm 배포까지 완료)
5. **fluent-bit TLS 캐시 버그 재발 방지 문서화** — add-on 설정을 바꿀 때마다 `kubectl -n amazon-cloudwatch rollout restart daemonset/fluent-bit` 필요하다는 점을 런북에 명시 (classic 유지 시 필수)

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

# kubelet cadvisor 엔드포인트 직접 확인 (핵심 판별 테스트)
kubectl get --raw "/api/v1/nodes/<node-name>/proxy/metrics/cadvisor"

# AmazonCloudWatchAgent CR (OTel pipeline 정의) 확인
kubectl get amazoncloudwatchagents.cloudwatch.aws.amazon.com -n amazon-cloudwatch -o yaml

# 계정 전체 메트릭 중 Container Insights 스타일 지표 존재 여부 확인
aws cloudwatch list-metrics --region ap-northeast-2 --output json \
  | python3 -c "
import json,sys
d = json.load(sys.stdin)
names = sorted(set(m['MetricName'] for m in d['Metrics']))
hits = [n for n in names if any(k in n.lower() for k in ['node_cpu','node_memory','pod_cpu','pod_memory','node_filesystem','cluster_node_count'])]
print(len(hits), 'container insights style metrics found')
"

# ClusterName Dimension 기준 전수 조회 (최종 판별)
aws cloudwatch list-metrics --region ap-northeast-2 \
  --dimensions Name=ClusterName,Value=smtm-eks --output json

# classic Container Insights 활성화/원복 (테스트 후 복원)
aws eks update-addon --cluster-name smtm-eks --addon-name amazon-cloudwatch-observability \
  --region ap-northeast-2 --configuration-values '{"containerInsights":{"enabled":false}}' \
  --resolve-conflicts OVERWRITE

# 로그 확인
kubectl -n amazon-cloudwatch logs -l app.kubernetes.io/name=cloudwatch-agent --tail=200 --since=30m
kubectl -n amazon-cloudwatch logs -l k8s-app=fluent-bit --tail=200 --since=30m
kubectl -n kube-system logs -l app.kubernetes.io/name=eks-pod-identity-agent --tail=200 --since=1h | grep -i "ExpiredToken\|non recoverable"

# 노드 시계 동기화 확인 (SSM)
aws ssm start-session --target <instance-id> --region ap-northeast-2
# 세션 내부: timedatectl / chronyc tracking / journalctl -u kubelet --since "2 hours ago"

# --- 2.3.5 classic 재검증 시 사용한 명령어 ---

# fluent-bit 재기동 및 완료 대기 (add-on 설정 변경 후 매번 필요)
kubectl -n amazon-cloudwatch rollout restart daemonset/fluent-bit
kubectl -n amazon-cloudwatch rollout status daemonset/fluent-bit

# containerinsights 전체 로그 그룹 존재/용량 확인
# 주의: storedBytes는 실시간 집계가 아니라 지연되는 메터링 게이지이므로,
# 재기동 직후 0으로 나와도 바로 "실패"로 판단하지 말고 아래 get-metric-statistics로 교차 확인할 것
aws logs describe-log-groups --region ap-northeast-2 \
  --log-group-name-prefix "/aws/containerinsights/smtm-eks" \
  --query 'logGroups[].{name:logGroupName,bytes:storedBytes}' --output table

# 실제 데이터포인트 존재 여부 직접 확인 (list-metrics 등재만으로는 실시간 여부 보장 안 됨, storedBytes보다 신뢰도 높음)
aws cloudwatch get-metric-statistics --region ap-northeast-2 \
  --namespace ContainerInsights --metric-name node_cpu_utilization \
  --dimensions Name=ClusterName,Value=smtm-eks \
  --start-time $(date -u -d '20 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 --statistics Average

# HPA 관련 replicas_desired / replicas_ready 확인 (ClusterName+Namespace+PodName 3개 디멘션 모두 필요)
aws cloudwatch get-metric-statistics --region ap-northeast-2 \
  --namespace ContainerInsights --metric-name replicas_desired \
  --dimensions Name=ClusterName,Value=smtm-eks Name=Namespace,Value=smtm Name=PodName,Value=smtm-backend \
  --start-time $(date -u -d '20 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 --statistics Average
```

---

## 6. 최종 결론 (Executive Summary)

### 6.1 "추천(OTel)" 방식을 선택해서 벌어진 상황

- EKS add-on 설정 화면에서 "OTel Container Insights - 추천" 라디오 버튼이 기본으로 강조되어 있어 이를 선택했음
- 실제로는 이 기능이 2026-04-02 출시된 **Public Preview**이고, 지원 리전이 US East(N. Virginia)/US West(Oregon)/Sydney/Singapore/Ireland 5곳뿐이며 **서울(`ap-northeast-2`)은 포함되지 않음**
- 이 사실을 모른 채 몇 주간 인증/권한 문제로 오인해 접근함: Pod Identity 자격증명 캐시 문제(2.1, 2.2)를 실제로 발견해 고쳤고, kubelet cadvisor·RBAC·OTel Collector 배선·IAM 정책까지 전부 정상 확인했지만 Node/Pod 지표는 계속 0건
- Export 로그에 "Partial success"가 찍혀서 통신 자체는 되는 것처럼 보였던 것도 혼란을 가중시킴 — 실제로는 서울 리전에 Preview 백엔드 처리 로직이 없어 요청은 받아주는 척하고 실제 저장은 안 되는 상태였음
- 결국 AWS 공식 발표를 찾아본 뒤에야 "클라이언트 쪽을 아무리 고쳐도 해결 불가능한, 리전 자체의 제약"이라는 근본 원인을 확인함

### 6.2 클래식(Enhanced) 방식으로 전환 후 진행한 것

- `containerInsights.enabled:true`, `otelContainerInsights.enabled:false`로 add-on 재구성
- 설정을 바꿀 때마다 cloudwatch-agent 파드의 내부 TLS 인증서가 재발급되는데 fluent-bit가 이를 자동으로 못 따라가 통신이 끊기는 버그를 발견 → `kubectl rollout restart daemonset/fluent-bit`로 해결(이 add-on 조합에서는 설정 변경 시마다 반복 필요한 구조적 이슈로 확정)
- 첫 검증에서 `performance` 로그 그룹의 `storedBytes`만 보고 0이라 "classic도 실패"로 오판 → 재검증 결과 이는 (a) fluent-bit 재기동 반영 미확인, (b) storedBytes 메터링 지연 두 가지가 겹친 오판이었음이 확인됨
- `list-metrics`(3,657개 항목, 90여 종 지표명)과 `get-metric-statistics`(실제 최근 데이터포인트)로 교차 검증한 결과, Node/Pod/Container 리소스 지표와 HPA용 `replicas_desired`/`replicas_ready`까지 1분 간격으로 정상 수집 중임을 확정
- 콘솔 "찾아보기" 화면에서 `smtm-backend` 파드 기준 33개 지표(CPU/메모리/네트워크/상태/재시작/HPA)가 전량 잡히는 것도 추가로 재확인

### 6.3 최종 결론

- **OTel Container Insights**: 서울 리전 미지원 Public Preview라 사용 불가 확정. 리전이 지원 목록에 추가되기 전까지 재시도 불필요
- **Classic Container Insights**: 정상 동작 확인 완료. Node/Pod/Container 지표는 물론 HPA 시각화(`replicas_desired`/`replicas_ready`)까지 CloudWatch만으로 전부 확보 가능 → 애초에 "Container Insights엔 HPA 지표가 없다"던 전제(3장 당초 근거)가 틀렸었음
- **모니터링 스택은 CloudWatch(classic Container Insights)와 kube-prometheus-stack 둘 다 구축**하기로 최종 결정. 역할 분리: CloudWatch Dashboard + Alarm→SNS는 운영 알림용, kube-prometheus-stack(Grafana)은 데모·시각화용
- **운영 시 반드시 지킬 것**: classic을 유지하는 한, add-on `configuration-values`를 변경할 때마다 `kubectl -n amazon-cloudwatch rollout restart daemonset/fluent-bit`를 함께 실행할 것 (안 하면 TLS 인증서 불일치로 데이터 수집이 끊김)
