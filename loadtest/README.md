# 부하테스트 (HPA·오토스케일 검증)

EKS에 배포된 backend/frontend의 HPA(Min 2 / Max 6 / CPU 65%)와
Cluster Autoscaler(Node Min 2 / Max 4, t3.medium)가 트래픽 증가에
제대로 반응하는지 관측하는 것이 목적이다. (팀 로드맵 "HPA·Cluster
Autoscaler·장애 복구 검증" 항목)

읽기 전용 부하라 데이터를 변경하지 않는다.

## 테스트 환경

| 구분 | 값 |
|---|---|
| 부하 발생지(k6 실행 위치) | 로컬 맥북 (macOS, Apple Silicon) |
| 부하 도구 | k6 v2.1.0 |
| 경로 | 로컬 맥 → 공인 인터넷 → `smtm-alb`(Internet-facing ALB, HTTPS) |
| 대상 리전 | `ap-northeast-2` (서울) |
| 대상 엔드포인트 | `https://smtm.mang.pe.kr` |

> 참고: 부하를 로컬 맥에서 공인 인터넷으로 보내므로 측정된 응답시간(p95)에는
> 인터넷 왕복 지연이 포함된다. HPA 스케일아웃 유발/관측(CPU 임계값 돌파)에는
> 충분하지만, 순수 서버 응답시간을 재려면 같은 리전의 CloudShell/EC2에서
> 실행해 클라이언트 구간 변수를 줄이는 편이 낫다.

## 준비

```bash
brew install k6

# 관측용 kubectl 접근 (계정 061039804626 자격증명 필요)
aws configure set region ap-northeast-2
aws eks update-kubeconfig --region ap-northeast-2 --name smtm-eks
```

## 실행

터미널 2개를 쓴다.

**터미널 A — 관측**
```bash
watch -n2 'kubectl -n smtm get hpa,pod -o wide'
# CPU/노드까지 보려면:
#   kubectl -n smtm top pod
#   kubectl top node
```

**터미널 B — 부하**
```bash
export BASE_URL=https://smtm.mang.pe.kr
export SMTM_USER=<시연용 계정>
export SMTM_PASS=<비밀번호>
export YEAR_MONTH=202607        # 데이터가 있는 달로

k6 run hpa-scaleout.js
```

## 무엇을 볼 것인가

부하 곡선은 약 14분(상승 5분 → 유지 4분 → 하강 5분).

1. **HPA REPLICAS**: 2 → ... → 6 으로 오르는가. 몇 분 만에 오르는가.
2. **파드 Pending**: 백엔드+프론트가 각각 최대 6이면 최대 12파드인데
   노드는 최대 4대(t3.medium = 2 vCPU)다. 파드가 `Pending`에 걸리면
   노드 부족 → Cluster Autoscaler가 노드를 늘리는 데 1~3분 걸리고
   그동안 응답시간이 튄다. **이 구간이 이번 검증의 핵심 관찰 포인트.**
3. **k6 요약의 p95 / http_req_failed**: 스케일아웃 완료 후 p95가 다시
   내려오는지. threshold(p95<800ms, err<2%)를 통과하는지.
4. **스케일인**: 부하가 빠진 뒤 파드가 다시 2로 줄어드는가 (HPA
   scaleDown 안정화 윈도우 기본 5분이라 곧바로 줄지 않는 게 정상).

## 알려진 주의점

- **정기결제 스케줄러**: 새 backend 파드가 뜰 때마다 기동 시점에
  정기결제 생성 로직이 돈다. 여기서 예외가 나면 파드가 죽어
  스케일아웃 실패처럼 보일 수 있으니, `kubectl -n smtm logs`로
  새 파드 로그를 함께 확인한다.
- **강도 조절**: 스케일아웃이 안 일어나면 stages의 target VU를 올리고,
  너무 빨리 6까지 가면 낮춘다. CPU 65% 근처를 완만히 지나야 관측이 는다.
