# ShowMeTheMoney EKS 전환 신규 리소스 구성 명세

## 1. 문서 목적과 범위

이 문서는 기존 `team4` EC2·Docker Compose·RDS 인프라에서 `smtm` EKS 인프라로 전환하기 위해 새로 추가한 리소스만 분리해 정리한 문서입니다.

다음 범위를 포함합니다.

- 네트워크 확장 리소스
- EKS 컴퓨팅 리소스
- ECR 리소스
- EKS IAM Role과 Policy
- EKS Add-on 및 Controller
- ALB, HTTPS, DNS 리소스
- Kubernetes Namespace 및 Helm 리소스

기존 `team4-vpc`, RDS, DB Subnet, DB Subnet Group, RDS 관리형 Secret은 재사용하며 신규 리소스가 아닙니다. 다만 신규 리소스와의 연결 관계를 이해할 수 있도록 의존성 항목에는 표시합니다.

## 2. 명명과 태그 기준

| 구분 | 기준 |
|---|---|
| 신규 리소스 접두사 | `smtm` |
| 신규 리소스 공통 태그 | `Version=smtm-v1` |
| 기존 공유 리소스 표시 | `Shared=smtm-v1` |
| Kubernetes Namespace | `smtm` |
| EKS Cluster 이름 | `smtm-eks` |
| 서비스 도메인 | `smtm.mang.pe.kr` |

AWS 기능 태그는 사용자 정의 공통 태그와 별도로 적용합니다.

```text
kubernetes.io/role/elb = 1
kubernetes.io/role/internal-elb = 1
kubernetes.io/cluster/smtm-eks = shared
```

## 3. 전환 구성의 의존 관계

```text
기존 team4-vpc와 RDS
  ├─ 신규 Public/Private App Subnet
  ├─ NAT Gateway A/B와 Route Table
  ├─ EKS Cluster + Managed Node Group
  │   ├─ Managed Add-on
  │   ├─ Pod Identity Role
  │   └─ Helm Controller
  ├─ ECR Backend/Frontend Image
  ├─ Helm Application
  │   ├─ Deployment/Service/HPA/PDB
  │   ├─ ExternalSecret
  │   └─ Ingress
  └─ ALB + ACM + 가비아 CNAME
```

---

# Part A. 신규 네트워크 리소스

## 4. Public Subnet B

| 항목 | 값 |
|---|---|
| 이름 | `smtm-public-b` |
| VPC | `team4-vpc` |
| AZ | `ap-northeast-2b` |
| CIDR | `10.21.1.0/24` |
| Public IPv4 자동 할당 | 활성화 |
| Subnet ID | `subnet-04012811b62580c6f` |
| Route Table | 기존 `team4-public-rt` |
| 태그 | `Version=smtm-v1` |
| 기능 태그 | `kubernetes.io/role/elb=1`, `kubernetes.io/cluster/smtm-eks=shared` |

기존에는 Public Subnet이 AZ-a에만 있었습니다. Internet-facing ALB와 NAT Gateway를 두 AZ에 배치하기 위해 AZ-b Public Subnet을 추가했습니다.

## 5. Private App Subnet A/B

| 항목 | `smtm-private-app-a` | `smtm-private-app-b` |
|---|---|---|
| VPC | `team4-vpc` | `team4-vpc` |
| AZ | `ap-northeast-2a` | `ap-northeast-2b` |
| CIDR | `10.21.10.0/24` | `10.21.11.0/24` |
| Public IPv4 자동 할당 | 비활성화 | 비활성화 |
| 용도 | EKS Node/Pod | EKS Node/Pod |
| 태그 | `Version=smtm-v1` | `Version=smtm-v1` |
| 기능 태그 | `kubernetes.io/role/internal-elb=1`, Cluster shared | 동일 |

Node와 Pod를 Public Subnet에 직접 배치하지 않고 Private App Subnet에 배치하여 인터넷의 직접 Inbound 접근을 차단합니다.

## 6. NAT Elastic IP 및 NAT Gateway

| 리소스 | 이름 | 배치/연결 | 목적 |
|---|---|---|---|
| Elastic IP | `smtm-nat-eip-a` | `smtm-nat-a` | AZ-a Outbound Source IP |
| Elastic IP | `smtm-nat-eip-b` | `smtm-nat-b` | AZ-b Outbound Source IP |
| NAT Gateway | `smtm-nat-a` | 기존 `team4-public-a` | Private App A의 인터넷 Outbound |
| NAT Gateway | `smtm-nat-b` | `smtm-public-b` | Private App B의 인터넷 Outbound |

NAT용 EIP는 서비스 도메인이나 ALB에 연결하지 않습니다. NAT Gateway가 외부로 요청을 보낼 때 사용하는 고정 Source Public IP입니다.

## 7. Private App Route Table

| 이름 | Route | 연결 |
|---|---|---|
| `smtm-private-app-rt-a` | `10.21.0.0/16 → local`, `0.0.0.0/0 → smtm-nat-a` | `smtm-private-app-a` |
| `smtm-private-app-rt-b` | `10.21.0.0/16 → local`, `0.0.0.0/0 → smtm-nat-b` | `smtm-private-app-b` |

두 AZ에 NAT를 하나씩 두면 한 AZ 또는 NAT Gateway 장애가 다른 AZ의 Outbound 경로까지 중단시키는 문제를 줄일 수 있습니다.

## 8. 신규/자동 Security Group

| Security Group | 생성 주체 | 용도 |
|---|---|---|
| `eks-cluster-sg-smtm-eks-*` | EKS 자동 생성 | Control Plane과 Node 통신 |
| EKS Node SG | Node Group/Launch Template | Node/Pod 통신과 RDS 접근 Source |
| ALB SG | AWS Load Balancer Controller | 인터넷 80/443 수신과 Pod Target 전송 |

기존 `team4-rds-sg`에는 MySQL 3306 Source로 EKS Node/Cluster SG를 추가했습니다. `0.0.0.0/0` 또는 VPC CIDR 전체를 RDS Source로 지정하지 않습니다.

---

# Part B. EKS 컴퓨팅 리소스

## 9. EKS Cluster

| 항목 | 값 |
|---|---|
| 이름 | `smtm-eks` |
| VPC | `team4-vpc` |
| Subnet | `smtm-private-app-a`, `smtm-private-app-b` |
| Cluster Role | `smtm-eks-cluster-role` |
| Endpoint | Public + Private |
| Access 관리 | Access Entry + EKS Access Policy |
| Log | EKS Control Plane Log → CloudWatch |
| 태그 | `Version=smtm-v1` |
| 검증 시 Node 버전 | `v1.36.2-eks-8f14419` |

EKS Cluster는 Kubernetes Control Plane을 AWS 관리형 서비스로 제공합니다. Control Plane EC2는 사용자 Account의 EC2 목록에 나타나지 않으며 Cluster 시간당 비용이 별도로 발생합니다.

## 10. Managed Node Group

| 항목 | 값 |
|---|---|
| 이름 | `smtm-app-mng` |
| Cluster | `smtm-eks` |
| Node Role | `smtm-eks-node-role` |
| Subnet | Private App A/B |
| Capacity Type | On-Demand |
| AMI | Amazon Linux 2023 x86_64 |
| Scaling | Min 2 / Desired 2 / Max 4 |
| 원격 SSH | 비활성화 |
| 태그 | `Version=smtm-v1` |

검증된 Node 배치는 다음과 같습니다.

| Node | AZ | 상태 |
|---|---|---|
| `ip-10-21-10-232.ap-northeast-2.compute.internal` | `ap-northeast-2a` | Ready |
| `ip-10-21-11-123.ap-northeast-2.compute.internal` | `ap-northeast-2b` | Ready |

## 11. Launch Template와 파생 EC2 리소스

| 리소스 | 이름/형태 | 설정 |
|---|---|---|
| Launch Template | `smtm-node-lt` | Node 부팅값과 Resource Tag 관리 |
| EC2 Instance | Managed Node Group가 자동 생성 | `Name=smtm-eks-node`, `Version=smtm-v1` 권장 |
| EBS Volume | Node와 함께 자동 생성 | gp3 30 GiB, 암호화, 태그 전파 |
| ENI | VPC CNI가 자동 생성 | Node와 Pod의 VPC IP |
| Auto Scaling Group | `eks-smtm-app-mng-*` | Node Min/Desired/Max 유지 |

Launch Template Version과 `Version=smtm-v1` 태그는 이름만 비슷할 뿐 서로 다른 속성입니다.

---

# Part C. ECR 리소스

## 12. Private Repository

| Repository | Image URI | Image |
|---|---|---|
| `smtm-backend` | `061039804626.dkr.ecr.ap-northeast-2.amazonaws.com/smtm-backend:<TAG>` | Spring Backend |
| `smtm-frontend` | `061039804626.dkr.ecr.ap-northeast-2.amazonaws.com/smtm-frontend:<TAG>` | Next.js Frontend |

공통 설정은 다음과 같습니다.

| 항목 | 값 |
|---|---|
| Repository 유형 | Private |
| Scan on push | 활성화 |
| Tag immutability | 활성화 권장 |
| Encryption | AES-256 |
| Resource Tag | `Version=smtm-v1` |
| 현재 검증 Tag | `pr11-review-p1` |
| 운영 권장 Tag | `sha-<Git commit SHA>` |

EC2에서 사용하던 Local Image를 동일 Account/Region의 ECR에 로그인한 뒤 Repository별 URI로 Tag하여 Push했습니다. Repository Tag는 비용·검색·관리 구분용 메타데이터이며 Docker Image Tag와는 다른 값입니다.

---

# Part D. EKS IAM Role과 Policy

## 13. Cluster Role

| 항목 | 값 |
|---|---|
| 이름 | `smtm-eks-cluster-role` |
| Trusted Service | `eks.amazonaws.com` |
| Policy | `AmazonEKSClusterPolicy` |
| 태그 | `Version=smtm-v1` |

EKS Control Plane이 Cluster 관련 AWS Network Resource를 관리할 때 사용합니다.

## 14. Node Role

| 항목 | 값 |
|---|---|
| 이름 | `smtm-eks-node-role` |
| Trusted Service | `ec2.amazonaws.com` |
| Policy | `AmazonEKSWorkerNodePolicy` |
| Policy | `AmazonEC2ContainerRegistryPullOnly` |
| 태그 | `Version=smtm-v1` |

Node가 Cluster에 등록되고 ECR에서 이미지를 Pull할 수 있게 합니다. `AmazonEKS_CNI_Policy`는 CNI 전용 Role 연결 전 초기 부팅에만 임시로 사용할 수 있으며 최종 구성에서는 `smtm-vpc-cni-role`로 분리합니다.

## 15. Pod Identity 공통 Trust

다음 Role들은 모두 IAM Policy가 아니라 IAM Role의 Trust Policy에서 `pods.eks.amazonaws.com`을 신뢰합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "pods.eks.amazonaws.com"
      },
      "Action": [
        "sts:AssumeRole",
        "sts:TagSession"
      ]
    }
  ]
}
```

이 JSON 자체를 `AmazonEKS_CNI_Policy`라는 Customer managed policy로 생성하는 것이 아닙니다. 각 Pod Identity Role을 만들 때 신뢰 정책으로 사용합니다.

## 16. Pod Identity Role 목록

| Role | 연결 Namespace/ServiceAccount | Policy | 최소 권한 목적 |
|---|---|---|---|
| `smtm-vpc-cni-role` | `kube-system/aws-node` | `AmazonEKS_CNI_Policy` | ENI와 Pod IP 관리 |
| `smtm-lbc-role` | `kube-system/aws-load-balancer-controller` | `smtm-lbc-policy` | ALB/TG/Listener/SG 생성·관리 |
| `smtm-eso-role` | `external-secrets/external-secrets` | `smtm-eso-policy` | 지정 Secret Get/Describe |
| `smtm-observability-role` | `amazon-cloudwatch/cloudwatch-agent` | CloudWatch Observability 권한 | Log/Metric 전송 |
| `smtm-cluster-autoscaler-role` | `kube-system/cluster-autoscaler` | `smtm-cluster-autoscaler-policy` | ASG 조회·용량 조정 |

### 16.1 LBC Policy의 주요 권한

- EC2 VPC/Subnet/Security Group 조회
- ELBv2 Load Balancer, Target Group, Listener, Rule 관리
- Target 등록/해제
- 필요한 Security Group Rule 관리
- ACM/WAF/Shield 정보 조회는 사용하는 Annotation 범위에 맞게 제한

### 16.2 ESO Policy의 주요 권한

- `secretsmanager:DescribeSecret`
- `secretsmanager:GetSecretValue`
- Resource는 기존 RDS 관리형 Secret과 `smtm/application`에 한정
- Customer managed KMS Key를 쓸 때만 해당 Key의 `kms:Decrypt` 추가

### 16.3 Cluster Autoscaler Policy의 주요 권한

- Auto Scaling Group 및 Launch Configuration 조회
- EKS Node Group 태그를 가진 ASG의 Desired Capacity 조정
- Scale-in 시 Instance 종료

## 17. 확인된 Pod Identity Association

| Namespace | ServiceAccount | IAM Role |
|---|---|---|
| `kube-system` | `aws-node` | `smtm-vpc-cni-role` |
| `kube-system` | `cluster-autoscaler` | `smtm-cluster-autoscaler-role` |
| `amazon-cloudwatch` | `cloudwatch-agent` | `smtm-observability-role` |
| `external-secrets` | `external-secrets` | `smtm-eso-role` |
| `kube-system` | `aws-load-balancer-controller` | `smtm-lbc-role` |

---

# Part E. EKS Add-on 및 Controller

## 18. Managed/Community Add-on

| 구성요소 | 관리 방식 | 사용 이유 |
|---|---|---|
| VPC CNI | EKS Managed Add-on | Pod를 VPC Network에 직접 연결 |
| CoreDNS | EKS Managed Add-on | Kubernetes Service 이름 해석 |
| kube-proxy | EKS Managed Add-on | Service Traffic 전달 |
| Pod Identity Agent | EKS Managed Add-on | Pod에 임시 AWS Credential 제공 |
| CloudWatch Observability | EKS Managed Add-on | Container Insights와 Log/Metric 수집 |
| Metrics Server | Community Add-on 또는 Helm | HPA가 CPU/Memory 사용량 조회 |

`kubectl top nodes`가 실제 CPU/Memory 값을 출력했으므로 Metrics API가 정상 동작하는 것을 확인했습니다.

## 19. Helm Controller

| Controller | Namespace | Replica/상태 | 사용 이유 |
|---|---|---|---|
| AWS Load Balancer Controller | `kube-system` | 2/2 Ready | Ingress에서 ALB와 Target Group 생성 |
| External Secrets Operator | `external-secrets` | Controller/Webhook/Cert Controller Ready | AWS Secret을 K8s Secret으로 동기화 |
| Cluster Autoscaler | `kube-system` | Deployment 정상 확인 필요 | Pending Pod에 맞춰 Managed Node Group 확장 |

Kube State Metrics는 Prometheus 관측용 Object Metric 도구이며 Metrics Server를 대체하지 않습니다.

---

# Part F. ALB, HTTPS 및 DNS 리소스

## 20. ACM 인증서

| 항목 | 값 |
|---|---|
| Domain | `smtm.mang.pe.kr` |
| Region | `ap-northeast-2` |
| Validation | DNS |
| Algorithm | RSA 2048 |
| ARN | `arn:aws:acm:ap-northeast-2:061039804626:certificate/41469aa6-7735-4e69-b243-881bc1c82333` |
| Status | Issued |
| Tag | `Name=smtm-acm`, `Version=smtm-v1` |

ACM 검증용 CNAME은 인증서 발급을 위한 레코드이며 서비스 트래픽용 CNAME과 별개입니다.

## 21. ALB

| 항목 | 값 |
|---|---|
| 이름 | `smtm-alb` |
| DNS | `smtm-alb-2027061443.ap-northeast-2.elb.amazonaws.com` |
| Scheme | Internet-facing |
| IP Type | IPv4 |
| Target Type | IP |
| Public Subnet A | `subnet-0b9526c86dd1c11e2` |
| Public Subnet B | `subnet-04012811b62580c6f` |
| Listener | HTTP 80, HTTPS 443 |
| Redirect | 80 → 443 |
| Tag | `Version=smtm-v1` |

ALB는 Ingress Annotation을 기준으로 AWS Load Balancer Controller가 생성하고 이후에도 Controller가 관리합니다. 콘솔에서 수동으로 Listener/Target Group 설정을 변경하면 Kubernetes 선언과 Drift가 생길 수 있습니다.

## 22. Target Group

| 이름 | Target | Port | Health Check |
|---|---|---:|---|
| `k8s-smtm-smtmback-6338e4e27b` | Backend Pod IP 2개 | 8080 | `GET /api/health` |
| `k8s-smtm-smtmfron-afc722c97f` | Frontend Pod IP 2개 | 3000 | `GET /` |

Target Type이 `ip`이므로 ALB는 NodePort를 거치지 않고 Pod IP를 직접 Target으로 등록합니다.

## 23. 가비아 DNS

| 항목 | 값 |
|---|---|
| 서비스 Host | `smtm` |
| Type | CNAME |
| 값/위치 | `smtm-alb-2027061443.ap-northeast-2.elb.amazonaws.com.` |
| FQDN | `smtm.mang.pe.kr` |

EKS용 신규 EIP는 필요하지 않습니다. 서비스의 Public 진입점은 ALB이며 ALB의 DNS 이름을 CNAME으로 연결합니다.

---

# Part G. Kubernetes Namespace 및 Helm 리소스

## 24. Namespace와 Helm Release

| 리소스 | 이름 |
|---|---|
| Namespace | `smtm` |
| Backend Release | `smtm-backend` |
| Frontend Release | `smtm-frontend` |
| Backend Values | `helm/backend/values-smtm-v1.local.yaml` |
| Frontend Values | `helm/frontend/values-smtm-v1.local.yaml` |

`values-smtm-v1.local.yaml`에는 실제 Account ID, ARN, Subnet ID, RDS Endpoint가 들어갈 수 있으므로 Git 추적 대상에서 제외합니다. Secret 실제 값은 local values에도 기록하지 않습니다.

## 25. Backend Helm 리소스

| Kind | 이름/역할 | 주요 설정 |
|---|---|---|
| Deployment | `smtm-backend` | Replica 2, ECR Image, RollingUpdate |
| Service | `smtm-backend` | ClusterIP, 8080 |
| ConfigMap | Backend 비민감 환경변수 | DB Host/Port/Name, CORS, Profile |
| SecretStore | `smtm-aws-secretsmanager` | AWS Secrets Manager 연결 |
| ExternalSecret | `smtm-backend` | RDS/App Secret 매핑 |
| Secret | `smtm-backend-secret` | DB Username/Password, JWT Secret |
| HPA | Backend HPA | Min 2, Max 6, CPU 65% |
| PDB | Backend PDB | `minAvailable: 1` |

Backend ConfigMap의 주요 값은 다음과 같습니다.

| Key | 값 |
|---|---|
| `DB_HOST` | `team4-mysql.c3qme6c6e7fj.ap-northeast-2.rds.amazonaws.com` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `showmethemoney` |
| `DB_SSL_MODE` | `REQUIRED` |
| `CORS_ALLOWED_ORIGINS` | `https://smtm.mang.pe.kr` |
| `JWT_EXPIRATION` | `86400` |
| `SPRING_PROFILES_ACTIVE` | `prod` |

## 26. Frontend Helm 리소스

| Kind | 이름/역할 | 주요 설정 |
|---|---|---|
| Deployment | `smtm-frontend` | Replica 2, ECR Image, RollingUpdate |
| Service | `smtm-frontend` | ClusterIP, 3000 |
| HPA | Frontend HPA | Min 2, Max 6, CPU 65% |
| PDB | Frontend PDB | `minAvailable: 1` |
| Ingress | `smtm-frontend` | ALB, Host/Path Routing, ACM |

Frontend Image는 Build 시 Public API URL을 `https://smtm.mang.pe.kr`로 주입합니다.

## 27. Ingress

| 설정 | 값 |
|---|---|
| Ingress Class | `alb` |
| Host | `smtm.mang.pe.kr` |
| `/api` Backend | `smtm-backend:8080` |
| `/` Backend | `smtm-frontend:3000` |
| Load Balancer Name | `smtm-alb` |
| Scheme | `internet-facing` |
| Target Type | `ip` |
| Listen Ports | HTTP 80, HTTPS 443 |
| SSL Redirect | 443 |
| Certificate | ACM ARN |
| Subnets | Public A/B ID 명시 |
| Resource Tag | `Version=smtm-v1` |

## 28. Pod 가용성과 확장

| 기능 | Backend | Frontend |
|---|---|---|
| 기본 Replica | 2 | 2 |
| HPA Min/Max | 2/6 | 2/6 |
| CPU 목표 | 65% | 65% |
| PDB | Min Available 1 | Min Available 1 |
| RollingUpdate | Surge 1, Unavailable 0 | Surge 1, Unavailable 0 |
| AZ 분산 | Topology Spread | Topology Spread |
| Health Probe | `/api/health`, `/api/ping` | `/` |

HPA는 Pod 수를 조정하고 Cluster Autoscaler는 Node 수를 조정합니다. 두 기능은 서로 다른 계층의 확장 기능입니다.

---

# Part H. 연결 관계와 사용 여부

## 29. 기존 공유 리소스와 신규 리소스 연결

| 기존 공유 리소스 | 신규 리소스 연결 |
|---|---|
| `team4-vpc` | EKS, Node, ALB, NAT, Pod ENI |
| `team4-igw` | ALB 및 NAT Gateway Public 인터넷 경로 |
| `team4-public-a` | ALB AZ-a, NAT A |
| `team4-public-rt` | Public A/B |
| `team4-rds-sg` | EKS Node/Cluster SG Source 추가 |
| `team4-mysql` | Backend DB |
| RDS 관리형 Secret | ExternalSecret의 DB Credential Source |

## 30. EKS 전환으로 대체된 기존 구성

| 기존 | 신규 |
|---|---|
| EC2 1대 | EKS Managed Node Group 2대 이상 |
| Docker Compose | Kubernetes Deployment + Helm |
| Local Docker Image | ECR Private Image |
| EC2 EIP | ALB DNS |
| Nginx | ALB + Ingress |
| Certbot | ACM |
| `.env` 파일 | ConfigMap + ExternalSecret + Secret |
| 수동 컨테이너 재시작 | Kubernetes Self-healing |
| 수동 증설 | HPA + Cluster Autoscaler |

## 31. 현재 신규 구성에서 사용하지 않는 항목

- Route 53
- ExternalDNS
- Argo CD
- GitHub Actions CI/CD
- WAF
- Shield Advanced
- RDS Proxy
- EKS Auto Mode
- 애플리케이션용 S3
- Frontend/Backend PVC
- Node Public IP/SSH

가비아 DNS를 수동으로 사용하므로 Route 53과 ExternalDNS는 현재 필요하지 않습니다.

## 32. 구축 및 검증 완료 기준

- [x] Public Subnet B 및 Private App Subnet A/B 구성
- [x] NAT Gateway A/B와 Private Route Table 구성
- [x] ECR Backend/Frontend Image 등록
- [x] EKS Cluster Active
- [x] Managed Node 2개 Ready 및 2개 AZ 배치
- [x] Pod Identity Association 5개 연결
- [x] AWS Load Balancer Controller Ready
- [x] External Secrets Operator Ready
- [x] Metrics API 정상
- [x] RDS Secret과 Application Secret 확인
- [x] ACM Certificate Issued
- [x] Helm Lint/Template 통과
- [x] Backend/Frontend Helm 배포
- [x] Backend/Frontend Pod 각 2개 Running
- [x] ALB Listener 80/443 및 Redirect
- [x] Target Group Healthy
- [x] 가비아 CNAME 해석
- [x] HTTPS 도메인 접속 및 애플리케이션 기능 확인

## 33. 다음 고도화 우선순위

1. ECR Image Tag를 Git SHA 기반 불변 Tag로 표준화합니다.
2. HPA 부하 시험과 Cluster Autoscaler Node 확장 시험을 수행합니다.
3. 새 ReplicaSet도 두 AZ에 확실히 분산되도록 Topology Spread를 보완합니다.
4. ALB 5xx, Unhealthy Target, Pod Restart, RDS CPU/Storage Alarm을 구성합니다.
5. GitHub Actions에서 Build/Scan/Push하고 Argo CD가 배포하는 GitOps 구조를 후속 도입합니다.
6. 롤백 기간이 끝나면 기존 EC2/EIP의 중지 또는 삭제 여부를 결정합니다.
