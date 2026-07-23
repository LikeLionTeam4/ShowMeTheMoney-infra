# ShowMeTheMoney EC2·Docker Compose·RDS 및 EKS 전체 리소스 구성 명세

## 1. 문서 목적

이 문서는 ShowMeTheMoney 프로젝트에서 실제로 구성한 두 세대의 AWS 인프라를 한 문서에서 비교·관리하기 위한 리소스 명세입니다.

- 기존 인프라: `team4` 접두사의 EC2 + Docker Compose + RDS 기반 인프라
- 신규 인프라: `smtm` 접두사의 Amazon EKS 기반 인프라
- 공통 데이터 계층: 기존 VPC와 RDS를 신규 EKS 인프라에서도 재사용
- 작성 기준: EKS 애플리케이션 배포, ALB Target Health, HTTPS 및 도메인 접속 검증이 완료된 시점

이 문서에는 비밀번호, JWT Secret 실제 값, PEM 개인 키를 기록하지 않습니다. Account ID, ARN, Subnet ID와 같은 리소스 식별자는 인증 정보는 아니지만 외부 공개 저장소에 올릴 때는 마스킹을 권장합니다.

## 2. 공통 기준값

| 구분 | 값 |
|---|---|
| AWS Region | `ap-northeast-2` |
| AWS Account ID | `061039804626` |
| VPC CIDR | `10.21.0.0/16` |
| 기존 리소스 접두사 | `team4` |
| 신규 EKS 리소스 접두사 | `smtm` |
| 기존 공통 태그 | `Team=team4` |
| 신규 EKS 공통 태그 | `Version=smtm-v1` |
| 기존 공유 리소스 태그 | `Shared=smtm-v1` |
| DB Engine | Amazon RDS for MySQL |
| DB 이름 | `showmethemoney` |
| 기존 서비스 도메인 | `team4.mang.pe.kr` |
| EKS 서비스 도메인 | `smtm.mang.pe.kr` |
| DNS 사업자 | 가비아 |

`Version=smtm-v1`은 프로젝트 버전을 구분하는 사용자 정의 태그입니다. EKS 시작 템플릿의 숫자형 버전과는 별개입니다.

## 3. 전체 아키텍처 요약

### 3.1 기존 EC2 기반

```text
사용자
  → 가비아 A 레코드
  → EC2 Elastic IP
  → Nginx + Certbot
  → Frontend 컨테이너 :3000
  → Backend 컨테이너 :8080
  → RDS MySQL :3306
```

Frontend와 Backend는 한 대의 EC2에서 Docker Compose로 실행합니다. EC2 장애가 발생하면 두 애플리케이션이 함께 중단되는 단일 장애 지점이 존재합니다.

### 3.2 신규 EKS 기반

```text
사용자
  → 가비아 CNAME
  → Internet-facing ALB
  → Ingress 경로 기반 라우팅
      ├─ /     → Frontend Service → Frontend Pod 2개
      └─ /api  → Backend Service  → Backend Pod 2개
  → 기존 RDS MySQL :3306
```

EKS Worker Node와 Pod는 두 개의 가용 영역에 배치합니다. 애플리케이션 컨테이너 이미지는 ECR에 저장하고, 비밀정보는 Secrets Manager와 External Secrets Operator를 통해 Kubernetes Secret으로 동기화합니다.

---

# Part A. 기존 EC2·Docker Compose·RDS 기반 인프라

## 4. VPC 및 서브넷

| 리소스 | 이름 | 주요 설정 | 용도 |
|---|---|---|---|
| VPC | `team4-vpc` | `10.21.0.0/16`, DNS resolution 활성화, DNS hostnames 활성화 | 모든 기존 및 신규 리소스의 공통 네트워크 |
| Public Subnet A | `team4-public-a` | AZ `ap-northeast-2a`, `10.21.0.0/24`, Public IPv4 자동 할당 활성화 | 기존 EC2, NAT Gateway A, EKS ALB의 Public AZ-a |
| Private DB Subnet A | `team4-private-db-a` | AZ `ap-northeast-2a`, `10.21.20.0/24`, Public IPv4 자동 할당 비활성화 | RDS DB Subnet Group |
| Private DB Subnet B | `team4-private-db-b` | AZ `ap-northeast-2b`, `10.21.21.0/24`, Public IPv4 자동 할당 비활성화 | RDS DB Subnet Group |

Private DB Subnet은 RDS용이며 EC2 애플리케이션을 실행하는 Subnet이 아닙니다. RDS DB Subnet Group은 최소 두 개의 가용 영역에 속한 Subnet을 요구하므로 A/B 두 개를 구성했습니다.

## 5. Internet Gateway와 Route Table

| 리소스 | 이름 | 연결 및 Route |
|---|---|---|
| Internet Gateway | `team4-igw` | `team4-vpc`에 연결 |
| Public Route Table | `team4-public-rt` | `10.21.0.0/16 → local`, `0.0.0.0/0 → team4-igw` |
| Private DB Route Table | `team4-private-db-rt` | `10.21.0.0/16 → local`, 인터넷 기본 Route 없음 |

연결 관계는 다음과 같습니다.

| Route Table | 연결 Subnet |
|---|---|
| `team4-public-rt` | `team4-public-a`, 이후 추가된 `smtm-public-b` |
| `team4-private-db-rt` | `team4-private-db-a`, `team4-private-db-b` |

DB Subnet Route Table에는 Internet Gateway 또는 NAT Gateway를 기본 경로로 추가하지 않았습니다. 프로젝트 범위에서는 사용자 정의 Network ACL을 별도로 만들지 않고 VPC 기본 Network ACL을 사용했습니다.

## 6. Security Group

### 6.1 EC2 Security Group

| 항목 | 값 |
|---|---|
| 이름 | `team4-ec2-sg` |
| 연결 대상 | `team4-ec2` |
| SSH | TCP 22, 프로젝트 실습 정책상 `0.0.0.0/0` |
| HTTP | TCP 80, `0.0.0.0/0` |
| HTTPS | TCP 443, `0.0.0.0/0` |
| Outbound | 기본 전체 허용 |

Docker의 3000/8080 포트는 Nginx가 같은 호스트에서 프록시하므로 최종 서비스 구성에서는 인터넷에 직접 공개할 필요가 없습니다. 이전 실습 과정에서 임시로 공개했다면 운영 검증 후 해당 Inbound Rule을 제거하는 것이 맞습니다.

### 6.2 RDS Security Group

| 항목 | 값 |
|---|---|
| 이름 | `team4-rds-sg` |
| 연결 대상 | `team4-mysql` |
| MySQL/Aurora | TCP 3306 |
| 기존 허용 Source | `team4-ec2-sg` |
| EKS 전환 중 추가 Source | EKS Node 또는 EKS Cluster Security Group |
| 인터넷 전체 공개 | 사용하지 않음 |

RDS는 Public Access를 사용하지 않으며 Security Group 참조 방식으로 EC2와 EKS에서만 접근하도록 제한합니다.

## 7. EC2, EIP, Key Pair 및 EBS

### 7.1 EC2

| 항목 | 값 |
|---|---|
| 리소스 이름 | `team4-ec2` |
| AMI | Amazon Linux 2023 x86_64 |
| Instance Type | `t3.small` |
| Subnet | `team4-public-a` |
| Security Group | `team4-ec2-sg` |
| IAM Instance Profile | `team4-ec2-runtime-role` |
| Public IP | Elastic IP 연결 |
| Metadata | IMDSv2 필수 |
| 공통 태그 | `Team=team4` |

### 7.2 Elastic IP

| 항목 | 값 |
|---|---|
| 이름 | `team4-eip` |
| 연결 대상 | `team4-ec2`의 기본 Private IP |
| 용도 | 고정 Public IPv4, 기존 가비아 A 레코드 대상 |
| EKS 전환 후 | 즉시 삭제하지 않고 롤백 기간 동안 보존 |

EIP는 Elastic IP 또는 탄력적 IP 주소를 의미합니다.

### 7.3 Key Pair

| 항목 | 값 |
|---|---|
| 이름 | `team4-key` |
| 용도 | EC2 SSH 인증 |
| 보안 기준 | PEM 파일을 Git, 문서, 메신저 공개 채널에 저장하지 않음 |

### 7.4 EBS

| 구분 | 이름 | 설정 | 용도 |
|---|---|---|---|
| Root Volume | EC2 생성 시 자동 생성 | gp3 30 GiB, 3000 IOPS, 125 MiB/s, 암호화, 종료 시 삭제 | OS, Docker Engine 및 기본 런타임 |
| Data Volume | `team4-ebs` | gp3 10 GiB, 암호화 | `/data`에 마운트하는 애플리케이션 데이터 영역 |

EC2 콘솔의 Name 열은 EBS의 `Name` 태그를 표시합니다. EC2 생성 시 자동 생성된 Root Volume과 별도로 생성한 Data Volume은 서로 다른 리소스입니다.

## 8. EC2 IAM Role 및 정책

### 8.1 IAM Role

| 항목 | 값 |
|---|---|
| Role 이름 | `team4-ec2-runtime-role` |
| Trusted Entity | `ec2.amazonaws.com` |
| 연결 방식 | EC2 Instance Profile |

### 8.2 연결 정책

| 정책 | 유형 | 용도 |
|---|---|---|
| `team4-ec2-runtime-policy` | Customer managed | 지정 ECR Repository의 이미지 Pull, 지정 Secrets Manager Secret 조회 |
| `AmazonSSMManagedInstanceCore` | AWS managed | Session Manager를 통한 EC2 관리 및 SSM 기능 |
| `CloudWatchAgentServerPolicy` | AWS managed | CloudWatch Agent의 Metric 및 Log 전송 |

`team4-ec2-runtime-policy`의 권한 범위는 다음과 같습니다.

| Service | Action | Resource 범위 |
|---|---|---|
| ECR | `ecr:GetAuthorizationToken` | `*`가 필수 |
| ECR | `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage` | 지정 Backend/Frontend Repository ARN |
| Secrets Manager | `secretsmanager:DescribeSecret`, `secretsmanager:GetSecretValue` | 지정 RDS 관리형 Secret ARN |

## 9. Docker Compose 애플리케이션 런타임

| 구성 | 이름 또는 값 | 설명 |
|---|---|---|
| Docker Engine | EC2에 설치 | 컨테이너 런타임 |
| Docker Compose | EC2에 설치 | Frontend/Backend 컨테이너 선언 및 실행 |
| Docker Network | `team4-network` | Compose 서비스 이름 기반 내부 통신 |
| Frontend Container | `showmethemoney-infra-frontend-1` | Host 3000 → Container 3000 |
| Backend Container | `showmethemoney-infra-backend-1` | Host 8080 → Container 8080 |
| Nginx | EC2 Host에 설치 | 80/443 수신, Frontend/Backend Reverse Proxy |
| Certbot | EC2 Host에 설치 | Let’s Encrypt 인증서 발급 및 자동 갱신 |
| Backend 환경 파일 | `/opt/team4/backend.env` | RDS 연결 및 JWT 설정; 파일 권한 제한 필요 |

Docker Compose를 사용하면 이미지를 동일한 실행 단위로 배포하고 환경 차이를 줄일 수 있지만, 한 EC2에 모든 컨테이너가 있으므로 노드 수준의 고가용성과 자동 복구는 제공하지 않습니다.

## 10. RDS와 Secrets Manager

### 10.1 DB Subnet Group

| 항목 | 값 |
|---|---|
| 이름 | `team4-db-subnet-group` |
| Subnet A | `team4-private-db-a` |
| Subnet B | `team4-private-db-b` |
| 가용 영역 | `ap-northeast-2a`, `ap-northeast-2b` |

### 10.2 RDS DB Instance

| 항목 | 값 |
|---|---|
| DB Identifier | `team4-mysql` |
| Engine | MySQL 8.4 계열 |
| DB Instance Class | `db.t4g.micro` |
| Storage | gp3 20 GiB |
| Storage Autoscaling 최대값 | 100 GiB |
| Multi-AZ | 활성화 |
| Public Access | No |
| DB Subnet Group | `team4-db-subnet-group` |
| Security Group | `team4-rds-sg` |
| Initial DB Name | `showmethemoney` |
| Master Username | `team4_admin` |
| Credential Management | AWS Secrets Manager 관리형 |
| Backup Retention | 7일 |
| Deletion Protection | 활성화 |
| Endpoint | `team4-mysql.c3qme6c6e7fj.ap-northeast-2.rds.amazonaws.com` |
| Port | 3306 |

RDS는 기존 인프라와 신규 EKS 인프라가 공동으로 사용합니다. EKS 전환을 위해 새 RDS를 만들거나 데이터를 다시 이관하지 않았습니다.

### 10.3 RDS 관리형 Secret

| 항목 | 값 |
|---|---|
| 유형 | RDS가 관리하는 Master Credential Secret |
| ARN | `arn:aws:secretsmanager:ap-northeast-2:061039804626:secret:rds!db-798bac98-1947-49ef-95f7-08bcfa911ce6-Lz7bbk` |
| 포함 Key | `username`, `password` |
| Secret 실제 값 | 문서화하지 않음 |
| EKS 전환 후 | External Secrets Operator가 읽어 Kubernetes Secret으로 동기화 |

## 11. S3 사용 범위

MVP 애플리케이션 런타임은 S3 Bucket에 의존하지 않습니다. 따라서 Frontend/Backend 기능을 실행하기 위한 필수 리소스 목록에는 S3를 포함하지 않습니다.

다만 교육 과업에서 S3 Versioning과 Lifecycle Policy 적용 증적이 요구될 경우에는 애플리케이션과 분리된 평가용 Bucket을 사용할 수 있습니다.

| 구분 | 적용 여부 |
|---|---|
| 애플리케이션 파일 저장 | 사용하지 않음 |
| 정적 웹 호스팅 | 사용하지 않음 |
| Versioning/Lifecycle 실습 | 과업 증적용으로 별도 적용 가능 |
| EKS Backend Pod의 S3 Role | 현재 불필요 |

## 12. 기존 도메인, HTTPS 및 관측

| 리소스 또는 기능 | 구성 |
|---|---|
| DNS | 가비아 `team4.mang.pe.kr` A 레코드 → `team4-eip` |
| HTTP Reverse Proxy | EC2 Nginx |
| HTTPS | Certbot + Let’s Encrypt |
| EC2 Metric | CloudWatch 기본 Metric 및 CloudWatch Agent |
| RDS Metric | CloudWatch RDS Metric |
| Log 확인 | EC2 시스템/Docker/Nginx 로그 및 필요 시 CloudWatch Logs |

---

# Part B. EKS 기반 인프라

## 13. 기존 리소스 재사용 범위

다음 리소스는 삭제하거나 복제하지 않고 EKS에서도 재사용합니다.

| 기존 리소스 | EKS에서 재사용하는 이유 | 추가 태그 |
|---|---|---|
| `team4-vpc` | 기존 RDS와 EKS가 Private IP로 통신 | `Shared=smtm-v1` |
| `team4-igw` | Public Subnet의 ALB와 NAT 인터넷 경로 | `Shared=smtm-v1` |
| `team4-public-a` | ALB/NAT의 AZ-a Public Subnet | `Shared=smtm-v1` |
| `team4-public-rt` | Public A/B 인터넷 Route | `Shared=smtm-v1` |
| `team4-private-db-a/b` | 기존 RDS 배치 | `Shared=smtm-v1` |
| `team4-private-db-rt` | RDS Private Route | `Shared=smtm-v1` |
| `team4-db-subnet-group` | 기존 RDS Multi-AZ 구성 | `Shared=smtm-v1` |
| `team4-rds-sg` | EKS Backend의 RDS 접근 허용 | `Shared=smtm-v1` |
| `team4-mysql` | 동일 DB와 데이터 사용 | `Shared=smtm-v1` |
| RDS 관리형 Secret | 동일 DB Credential 사용 | `Shared=smtm-v1` |

`team4-ec2`, `team4-eip`, `team4-ebs`, `team4-key`, `team4-ec2-sg`, `team4-ec2-runtime-role`은 EKS가 직접 사용하지 않습니다. 이들은 전환 실패 시 복구하기 위한 기존 인프라로 일정 기간 보존합니다.

## 14. EKS 전환을 위해 추가한 네트워크

### 14.1 신규 Subnet

| 리소스 | 이름 | AZ | CIDR | Public IPv4 자동 할당 | 용도 |
|---|---|---|---|---|---|
| Public Subnet B | `smtm-public-b` | `ap-northeast-2b` | `10.21.1.0/24` | 활성화 | ALB와 NAT Gateway B |
| Private App Subnet A | `smtm-private-app-a` | `ap-northeast-2a` | `10.21.10.0/24` | 비활성화 | EKS Node/Pod |
| Private App Subnet B | `smtm-private-app-b` | `ap-northeast-2b` | `10.21.11.0/24` | 비활성화 | EKS Node/Pod |

확인된 Public Subnet ID는 다음과 같습니다.

| Subnet | ID |
|---|---|
| `team4-public-a` | `subnet-0b9526c86dd1c11e2` |
| `smtm-public-b` | `subnet-04012811b62580c6f` |

### 14.2 NAT Gateway

| 리소스 | 이름 | 배치 |
|---|---|---|
| NAT Elastic IP A | `smtm-nat-eip-a` | NAT Gateway A에 연결 |
| NAT Elastic IP B | `smtm-nat-eip-b` | NAT Gateway B에 연결 |
| NAT Gateway A | `smtm-nat-a` | `team4-public-a` |
| NAT Gateway B | `smtm-nat-b` | `smtm-public-b` |

Private App Subnet의 Node와 Pod는 Public IP가 없지만 ECR Image Pull, AWS API, 패키지 저장소 접근이 필요합니다. NAT Gateway는 이 Outbound 인터넷 연결을 제공하며 외부에서 Node로 직접 들어오는 Inbound 연결은 만들지 않습니다.

### 14.3 Private App Route Table

| Route Table | Route | 연결 Subnet |
|---|---|---|
| `smtm-private-app-rt-a` | `10.21.0.0/16 → local`, `0.0.0.0/0 → smtm-nat-a` | `smtm-private-app-a` |
| `smtm-private-app-rt-b` | `10.21.0.0/16 → local`, `0.0.0.0/0 → smtm-nat-b` | `smtm-private-app-b` |

### 14.4 EKS/ALB 기능 태그

| Subnet | 기능 태그 |
|---|---|
| `team4-public-a` | `kubernetes.io/role/elb=1`, `kubernetes.io/cluster/smtm-eks=shared` |
| `smtm-public-b` | `kubernetes.io/role/elb=1`, `kubernetes.io/cluster/smtm-eks=shared` |
| `smtm-private-app-a/b` | `kubernetes.io/role/internal-elb=1`, `kubernetes.io/cluster/smtm-eks=shared` |

`kubernetes.io/role/elb`와 `kubernetes.io/role/internal-elb`는 여러 팀이 같은 AWS Account를 사용해도 기능상 공통 Key를 사용하는 것이 정상입니다. 실제 클러스터 구분은 `kubernetes.io/cluster/smtm-eks=shared`와 Ingress에 명시한 Subnet ID로 수행합니다.

## 15. EKS Cluster와 Managed Node Group

### 15.1 EKS Cluster

| 항목 | 값 |
|---|---|
| Cluster 이름 | `smtm-eks` |
| 리전 | `ap-northeast-2` |
| Cluster IAM Role | `smtm-eks-cluster-role` |
| VPC | `team4-vpc` |
| Cluster Subnet | `smtm-private-app-a`, `smtm-private-app-b` |
| Endpoint 방식 | Public + Private 구성 |
| Access 관리 | EKS Access Entry 및 EKS Access Policy |
| Control Plane Log | CloudWatch Logs 연동 |
| 태그 | `Version=smtm-v1` |
| 검증 시 Node Kubernetes 버전 | `v1.36.2-eks-8f14419` |

팀원에게는 업무 범위에 따라 `AmazonEKSEditPolicy`, `AmazonEKSAdminPolicy`, `AmazonEKSClusterAdminPolicy`를 구분해 부여합니다. 모든 팀원에게 Cluster 전체 관리자 권한을 일괄 부여하는 구성은 권장하지 않습니다.

### 15.2 Managed Node Group

| 항목 | 값 |
|---|---|
| 이름 | `smtm-app-mng` |
| Node IAM Role | `smtm-eks-node-role` |
| Subnet | `smtm-private-app-a`, `smtm-private-app-b` |
| Capacity | On-Demand |
| OS/AMI 계열 | Amazon Linux 2023 x86_64 |
| 노드 수 | 최소 2, 희망 2, 최대 4 |
| 가용 영역 | `ap-northeast-2a`, `ap-northeast-2b` |
| SSH 원격 접속 | 비활성화 |
| 태그 | `Version=smtm-v1` |

배포 검증 시 Ready Node 2개가 각각 `ap-northeast-2a`와 `ap-northeast-2b`에서 동작했습니다.

### 15.3 Launch Template 및 자동 파생 리소스

| 구분 | 이름 또는 형태 | 설명 |
|---|---|---|
| Launch Template | `smtm-node-lt` | Node의 Instance/Volume/ENI 태그와 부팅 설정 관리 |
| EC2 Node | EKS/ASG가 자동 생성 | 실제 Worker Node |
| Node EBS | Launch Template/Node Group에서 생성 | gp3 30 GiB, 암호화 권장 |
| Auto Scaling Group | `eks-smtm-app-mng-*` 형태 | Managed Node Group 용량 관리 |
| ENI | VPC CNI가 자동 생성 | Pod에 VPC IP 할당 |
| Cluster SG | `eks-cluster-sg-smtm-eks-*` 형태 | EKS가 자동 생성 |
| Node SG | Node Group/Launch Template 연계 | Control Plane, Node, Pod 통신 |

AWS가 자동 생성하는 리소스는 이름 전체를 고정할 수 없습니다. 가능한 리소스에는 `Version=smtm-v1` 태그를 전파합니다.

## 16. ECR

| Repository | URI | 용도 |
|---|---|---|
| `smtm-backend` | `061039804626.dkr.ecr.ap-northeast-2.amazonaws.com/smtm-backend` | Spring Backend Image |
| `smtm-frontend` | `061039804626.dkr.ecr.ap-northeast-2.amazonaws.com/smtm-frontend` | Next.js Frontend Image |

권장 설정은 다음과 같습니다.

| 항목 | 값 |
|---|---|
| Visibility | Private |
| Tag immutability | 활성화 권장 |
| Scan on push | 활성화 |
| Encryption | AES-256 |
| 태그 | `Version=smtm-v1` |
| 검증 배포 Image Tag | `pr11-review-p1` |
| 운영 권장 Image Tag | `sha-<Git commit SHA>` |

`REGISTRY`는 Account와 Region의 ECR Registry 주소이며 Repository 이름까지 포함하지 않습니다. 실제 Push/Pull 대상은 `${REGISTRY}/smtm-backend:<TAG>` 또는 `${REGISTRY}/smtm-frontend:<TAG>`입니다.

## 17. EKS IAM Role과 Policy

### 17.1 Cluster 및 Node Role

| Role | 신뢰 주체 | 연결 Policy | 용도 |
|---|---|---|---|
| `smtm-eks-cluster-role` | `eks.amazonaws.com` | `AmazonEKSClusterPolicy` | EKS Control Plane의 AWS 리소스 관리 |
| `smtm-eks-node-role` | `ec2.amazonaws.com` | `AmazonEKSWorkerNodePolicy`, `AmazonEC2ContainerRegistryPullOnly` | Worker Node 등록, ECR Image Pull |

VPC CNI 전용 Role을 연결한 뒤에는 `AmazonEKS_CNI_Policy`를 Node Role에 중복 부여하지 않습니다.

### 17.2 Pod Identity Role

모든 Pod Identity Role은 `pods.eks.amazonaws.com`을 신뢰하고 `sts:AssumeRole`, `sts:TagSession`을 허용합니다.

| Role | Namespace | ServiceAccount | 연결 Policy/권한 | 용도 |
|---|---|---|---|---|
| `smtm-vpc-cni-role` | `kube-system` | `aws-node` | `AmazonEKS_CNI_Policy` | Pod ENI와 VPC IP 관리 |
| `smtm-lbc-role` | `kube-system` | `aws-load-balancer-controller` | `smtm-lbc-policy` | ALB, Target Group, Listener, 관련 SG 관리 |
| `smtm-eso-role` | `external-secrets` | `external-secrets` | `smtm-eso-policy` | 지정 Secrets Manager Secret 조회 |
| `smtm-observability-role` | `amazon-cloudwatch` | `cloudwatch-agent` | CloudWatch Observability 권한 | Container/Node Log 및 Metric 전송 |
| `smtm-cluster-autoscaler-role` | `kube-system` | `cluster-autoscaler` | `smtm-cluster-autoscaler-policy` | Node Group 용량 조회 및 조정 |

확인된 Pod Identity Association은 위 다섯 개이며, Namespace와 ServiceAccount 이름이 실제 Helm/Add-on 설치값과 정확히 일치해야 합니다.

## 18. EKS Add-on 및 Controller

| 구성요소 | 설치/관리 방식 | 역할 | 검증 기준 |
|---|---|---|---|
| Amazon VPC CNI | EKS Managed Add-on | Pod에 VPC IP 할당 | `aws-node` DaemonSet 정상 |
| CoreDNS | EKS Managed Add-on | Cluster 내부 DNS | CoreDNS Pod Ready |
| kube-proxy | EKS Managed Add-on | Kubernetes Service 네트워크 | DaemonSet 정상 |
| EKS Pod Identity Agent | EKS Managed Add-on | Pod Identity Credential 전달 | Agent DaemonSet 정상 |
| CloudWatch Observability | EKS Managed Add-on | Container Insights, Log/Metric 수집 | CloudWatch Agent 정상 |
| Metrics Server | EKS Community Add-on 또는 Helm | HPA용 CPU/Memory Resource Metric | `kubectl top nodes` 출력 |
| AWS Load Balancer Controller | Helm | Ingress를 ALB로 변환 | Deployment `2/2` Ready |
| External Secrets Operator | Helm | Secrets Manager 값을 K8s Secret으로 동기화 | Controller/Webhook/Cert Controller Ready |
| Cluster Autoscaler | Helm | Pending Pod에 맞춰 Node Group 확장 | Deployment 및 Pod Identity 정상 |

Kube State Metrics는 Kubernetes Object 상태를 Prometheus 형식으로 노출하는 도구이며 HPA가 사용하는 Metrics Server와 다른 구성요소입니다.

Amazon EBS CSI Driver가 실제 Add-on 목록에 설치되어 있더라도 현재 Frontend/Backend는 PVC를 사용하지 않으므로 애플리케이션 필수 의존성은 아닙니다. 콘솔의 일곱 번째 Managed Add-on 이름은 운영 인벤토리 확정 시 다시 확인해야 합니다.

## 19. Secrets Manager와 Kubernetes Secret 연동

### 19.1 AWS Secrets Manager

| Secret | 포함 정보 | 소비 주체 |
|---|---|---|
| 기존 RDS 관리형 Secret | `username`, `password` | External Secrets Operator |
| `smtm/application` | `JWT_SECRET` | External Secrets Operator |

`smtm/application`은 `Version=smtm-v1` 태그를 사용하며 기본 AWS 관리형 Secrets Manager KMS Key를 사용합니다. Secret 실제 값은 Git과 Helm values에 기록하지 않습니다.

### 19.2 Kubernetes Secret 리소스

| 리소스 | Namespace | 역할 |
|---|---|---|
| `SecretStore/smtm-aws-secretsmanager` | `smtm` | AWS Secrets Manager Provider와 Region 정의 |
| `ExternalSecret/smtm-backend` | `smtm` | 두 AWS Secret에서 필요한 Key 매핑 |
| `Secret/smtm-backend-secret` | `smtm` | Backend Pod에 `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` 제공 |

## 20. ACM, ALB, Target Group 및 DNS

### 20.1 ACM

| 항목 | 값 |
|---|---|
| 인증 도메인 | `smtm.mang.pe.kr` |
| Region | `ap-northeast-2` |
| 검증 방식 | DNS 검증 |
| Key Algorithm | RSA 2048 |
| ARN | `arn:aws:acm:ap-northeast-2:061039804626:certificate/41469aa6-7735-4e69-b243-881bc1c82333` |
| 상태 | Issued |
| 태그 | `Name=smtm-acm`, `Version=smtm-v1` |

ALB가 사용하는 ACM 인증서는 반드시 ALB와 같은 Region에 있어야 합니다.

### 20.2 Application Load Balancer

| 항목 | 값 |
|---|---|
| 이름 | `smtm-alb` |
| DNS 이름 | `smtm-alb-2027061443.ap-northeast-2.elb.amazonaws.com` |
| Scheme | Internet-facing |
| IP Address Type | IPv4 |
| Subnet | Public A/B를 ID로 명시 |
| Listener | HTTP 80, HTTPS 443 |
| HTTP 처리 | HTTPS 443으로 Redirect |
| TLS 인증서 | `smtm.mang.pe.kr` ACM 인증서 |
| Target Type | IP |
| 태그 | `Version=smtm-v1` |

### 20.3 Target Group

| Target Group | Port | Health Check | 대상 |
|---|---:|---|---|
| `k8s-smtm-smtmback-6338e4e27b` | 8080 | `/api/health` | Backend Pod IP 2개 |
| `k8s-smtm-smtmfron-afc722c97f` | 3000 | `/` | Frontend Pod IP 2개 |

두 Target Group 모두 배포 검증 시 Healthy 상태를 확인했습니다. Target Group과 ALB Security Group은 AWS Load Balancer Controller가 Ingress를 기준으로 생성·관리합니다.

### 20.4 가비아 DNS

| 항목 | 값 |
|---|---|
| Host | `smtm` |
| Type | CNAME |
| 값/위치 | `smtm-alb-2027061443.ap-northeast-2.elb.amazonaws.com.` |
| 결과 | `smtm.mang.pe.kr` → ALB DNS |

ALB IP는 고정되지 않으므로 개별 ALB IP를 A 레코드로 등록하지 않습니다. 가비아가 요구하는 경우 CNAME 값 마지막에 `.`을 붙입니다.

## 21. Kubernetes Namespace와 Helm 리소스

### 21.1 공통

| 리소스 | 값 |
|---|---|
| Namespace | `smtm` |
| Backend Helm Release | `smtm-backend`, 검증 시 Revision 2 |
| Frontend Helm Release | `smtm-frontend`, 검증 시 Revision 1 |
| 공통 Label | `Version=smtm-v1` |

### 21.2 Backend

| 리소스 | 구성 |
|---|---|
| Deployment | Backend Pod 2개, RollingUpdate |
| Image | ECR `smtm-backend:pr11-review-p1` |
| Service | ClusterIP, Port 8080 |
| HPA | 최소 2, 최대 6, 목표 CPU 65% |
| PDB | `minAvailable: 1` |
| Resource Request | CPU 250m, Memory 512Mi |
| Resource Limit | CPU 500m, Memory 768Mi |
| Readiness Probe | `/api/health` |
| Liveness/Startup Probe | `/api/ping` |
| ConfigMap | DB Host/Port/Name, SSL Mode, CORS, JWT 만료시간, Spring Profile |
| Secret | DB Username/Password, JWT Secret |

### 21.3 Frontend

| 리소스 | 구성 |
|---|---|
| Deployment | Frontend Pod 2개, RollingUpdate |
| Image | ECR `smtm-frontend:pr11-review-p1` |
| Service | ClusterIP, Port 3000 |
| HPA | 최소 2, 최대 6, 목표 CPU 65% |
| PDB | `minAvailable: 1` |
| Resource Request | CPU 100m, Memory 256Mi |
| Resource Limit | CPU 300m, Memory 512Mi |
| Probe | `/` |

### 21.4 Ingress

| 항목 | 값 |
|---|---|
| 이름 | `smtm-frontend` |
| Ingress Class | `alb` |
| Host | `smtm.mang.pe.kr` |
| `/api` | `smtm-backend:8080` |
| `/` | `smtm-frontend:3000` |
| Scheme | `internet-facing` |
| Target Type | `ip` |
| SSL Redirect | 443 |

### 21.5 가용성과 배포 안정성

| 기능 | 적용 내용 |
|---|---|
| Replica | Frontend 2, Backend 2 |
| HPA | 부하에 따라 최대 6 Pod |
| PDB | 유지보수 중 각 서비스 최소 1 Pod 유지 |
| RollingUpdate | `maxSurge: 1`, `maxUnavailable: 0` |
| Topology Spread | Zone 우선 분산, Hostname 분산 |
| Startup Probe | 느린 Backend 시작 중 조기 재시작 방지 |

검증 과정에서 새 ReplicaSet Pod 두 개가 일시적으로 같은 AZ에 배치된 사례가 있었습니다. 최종적으로 Backend Pod를 두 AZ에 분산했으며, 향후 Helm 차트에는 `matchLabelKeys: [pod-template-hash]` 적용을 검토합니다.

## 22. CloudWatch 및 운영 관측

| 대상 | 확인 수단 |
|---|---|
| EKS Control Plane | EKS Control Plane Log Group |
| Node/Pod | CloudWatch Observability, Container Insights |
| Kubernetes Resource Metric | Metrics Server, `kubectl top` |
| ALB | RequestCount, TargetResponseTime, HTTPCode 4xx/5xx, HealthyHostCount |
| RDS | CPU, Connection, FreeStorageSpace, Read/Write Latency |
| Controller | LBC, ESO, Cluster Autoscaler Deployment Log |

CloudWatch Agent 정책을 EC2 Role에 붙이는 것과 EKS Observability Pod Identity Role을 구성하는 것은 서로 다른 실행 환경을 위한 설정입니다.

## 23. 기존 인프라와 EKS 인프라의 사용 리소스 비교

| 영역 | 기존 EC2 기반 | EKS 기반 | 고도화 효과 |
|---|---|---|---|
| Compute | EC2 1대 | Managed Node Group 2대 이상 | 노드/AZ 분산 및 자동 복구 |
| Container 실행 | Docker Compose | Kubernetes Deployment | 선언형 배포와 Replica 관리 |
| Image 저장 | EC2 Local/Docker Image | ECR Private Repository | 중앙 Image 저장·스캔·버전 관리 |
| 진입점 | EIP + Nginx | ALB + Ingress | Managed L7 Load Balancing |
| HTTPS | Certbot | ACM + ALB Listener | 인증서 관리의 AWS 이관 |
| DNS | A → EIP | CNAME → ALB | ALB 동적 IP 대응 |
| Scale-out | 수동 EC2 증설 | HPA + Cluster Autoscaler | Pod 및 Node 자동 확장 |
| 장애 대응 | EC2 장애 시 전체 중단 | Replica와 Multi-AZ Node | 애플리케이션 가용성 향상 |
| Secret | EC2 env 파일 | Secrets Manager + ESO + K8s Secret | Git/이미지에서 Secret 분리 |
| IAM | EC2 Instance Role | Cluster/Node/Pod Role 분리 | 최소 권한과 Workload 단위 권한 |
| Observability | EC2/RDS 기본 Metric | EKS/Pod/ALB/RDS Metric과 Log | 계층별 관측 가능 |
| DB | RDS Multi-AZ | 기존 RDS Multi-AZ 재사용 | 데이터 이관 위험 최소화 |

## 24. EKS에서 사용하지 않는 기존 기능과 미도입 기능

### 24.1 EKS 실행 경로에서 사용하지 않는 기존 리소스

- `team4-ec2`
- `team4-eip`
- `team4-ebs`
- `team4-key`
- `team4-ec2-sg`
- `team4-ec2-runtime-role`
- Docker Compose
- Nginx
- Certbot/Let’s Encrypt
- `team4.mang.pe.kr`

위 항목은 롤백용 기존 환경으로 보존할 수 있지만 신규 EKS 트래픽 경로에는 포함되지 않습니다.

### 24.2 현재 구성에 포함하지 않은 기능

- Route 53 Hosted Zone과 Route 53 Alias
- Argo CD
- GitHub Actions 기반 자동 Build/Push/Deploy
- AWS WAF
- Shield Advanced
- RDS Proxy
- Service Mesh
- EKS Auto Mode
- 애플리케이션용 S3
- 애플리케이션 PVC/Persistent EBS
- Worker Node Public IP 및 SSH

미도입 항목은 “누락”과 “현재 범위에서 의도적으로 제외”를 구분해야 합니다. 예를 들어 S3와 PVC는 현재 애플리케이션이 영속 파일 저장을 요구하지 않으므로 필수 리소스가 아닙니다.

## 25. 현재 검증 완료 상태

| 검증 항목 | 결과 |
|---|---|
| EKS Node 2개 Ready | 완료 |
| Node의 AZ-a/AZ-b 분산 | 완료 |
| AWS Load Balancer Controller `2/2` | 완료 |
| External Secrets Operator Controller/Webhook/Cert Controller | 완료 |
| Pod Identity Association 5개 | 완료 |
| Backend/Frontend Helm Lint | 통과 |
| Backend/Frontend Helm 배포 | 완료 |
| Backend/Frontend Pod 각 2개 Running | 완료 |
| ALB HTTP 80/HTTPS 443 Listener | 완료 |
| HTTP → HTTPS Redirect | 완료 |
| Backend/Frontend Target Group Healthy | 완료 |
| 가비아 CNAME 해석 | 완료 |
| `https://smtm.mang.pe.kr` 접속 | 완료 |
| 애플리케이션 기능 동작 | 완료 |

`HEAD /api/health`가 401을 반환한 것은 Spring Security가 GET만 허용한 경우 발생할 수 있습니다. ALB Health Check와 기능 검증은 `GET /api/health` 기준으로 판단합니다.

## 26. 운영 전 추가 권장사항

1. ECR Image Tag를 임시 `pr11-review-p1`에서 `sha-<commit>` 형태로 전환합니다.
2. HPA 동작을 실제 부하로 검증하고 Pod 확장·축소 이벤트를 증적합니다.
3. ReplicaSet 교체 중에도 AZ 분산이 유지되도록 Topology Spread 설정을 보완합니다.
4. CloudWatch Alarm을 ALB 5xx, Unhealthy Target, RDS CPU/Storage, Node/Pod 이상에 추가합니다.
5. 팀원 EKS Access Policy를 역할에 맞게 최소 권한으로 조정합니다.
6. 롤백 기간 종료 후 `team4` EC2/EIP 보존 여부를 결정하고 비용을 정리합니다.
7. Helm local values에 실제 ARN과 ID를 유지하되 Git 추적 대상에서 계속 제외합니다.
