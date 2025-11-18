# 🏠 로컬 개발 환경 사용 가이드

로컬 PC에서 Kafka 이벤트 아키텍처 시스템을 실행하는 완전한 가이드입니다.

## 📋 목차

- [빠른 시작 (자동)](#빠른-시작-자동)
- [수동 설정](#수동-설정)
- [일상적인 사용](#일상적인-사용)
- [문제 해결](#문제-해결)
- [개발 팁](#개발-팁)

---

## 빠른 시작 (자동)

### 1️⃣ 필수 요구사항

- **Docker Desktop** 설치됨
- **Git** 설치됨
- 최소 **8GB RAM**

### 2️⃣ 실행

```bash
# 프로젝트 클론
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab

# 자동 설정 실행
./setup-local.sh
```

### 3️⃣ 완료!

대시보드 접속: http://localhost:8080/dashboard

---

## 수동 설정

자동 설정 스크립트 대신 직접 설정하려면:

### Step 1: Docker Compose 실행

```bash
cd infra-local
docker-compose up -d --build
```

⏱️ 초기 빌드: 3-5분 소요

### Step 2: Kafka 토픽 생성

```bash
# orders 토픽
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic orders \
  --partitions 3 --replication-factor 1 \
  --if-not-exists

# order-stats 토픽
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-stats \
  --partitions 3 --replication-factor 1 \
  --if-not-exists
```

### Step 3: Server2 재시작

```bash
docker restart server2-streams
```

### Step 4: 헬스 체크

```bash
cd ..
./check-health.sh
```

---

## 일상적인 사용

### 시스템 시작

```bash
cd infra-local
docker-compose up -d
```

### 시스템 종료

```bash
cd infra-local
docker-compose down
```

### 완전 초기화 (데이터 삭제)

```bash
cd infra-local
docker-compose down -v
```

### 테스트 주문 생성

**방법 1: 웹 대시보드에서 생성** ⭐ (가장 쉬움!)

```bash
# 브라우저에서 접속
open http://localhost:8080/dashboard
```

대시보드에서:
- **🎲 랜덤 주문 생성** 버튼 클릭 → 즉시 랜덤 주문 생성!
- 또는 폼에 직접 입력하여 주문 생성

실시간으로 확인:
- 처리 흐름도 애니메이션 (Producer → KStream → ... → Output)
- KStream 이벤트 스트림 (파란색 배지)
- KTable 상태 업데이트 (빨간색 배지)
- 통계 즉시 반영

**방법 2: 쉘 스크립트로 대량 생성**

```bash
# 10개 주문 생성
./test-orders.sh 10

# 100개 주문 생성 (부하 테스트)
./test-orders.sh 100
```

### 통계 확인

```bash
# CLI로 확인
./check-stats.sh

# 브라우저로 확인
open http://localhost:8080/dashboard
```

### 로그 확인

```bash
# 전체 로그
cd infra-local
docker-compose logs -f

# 특정 서비스 로그
docker logs server1-app -f
docker logs server2-streams -f
docker logs kafka-broker -f
```

---

## 문제 해결

### ❌ Server2가 재시작을 반복

**원인**: Kafka 토픽이 없음

**해결**:
```bash
# 토픽 확인
docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list

# 없으면 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic orders \
  --partitions 3 --replication-factor 1

docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-stats \
  --partitions 3 --replication-factor 1

docker restart server2-streams
```

### ❌ 통계가 0으로 표시됨

**해결**:
```bash
# Server2 로그 확인
docker logs server2-streams --tail 50

# Server2 재시작
docker restart server2-streams

# 새 주문 생성
./test-orders.sh 5

# 통계 확인
./check-stats.sh
```

### ❌ 포트 충돌

**증상**: `Port is already allocated`

**해결**:
```bash
# 사용 중인 프로세스 확인
lsof -i :8080  # Server1
lsof -i :9092  # Kafka
lsof -i :5432  # PostgreSQL

# 프로세스 종료 후 재시작
```

### ❌ Docker 메모리 부족

**해결**:
1. Docker Desktop 설정 열기
2. Resources → Memory를 8GB 이상으로 증가
3. Apply & Restart

---

## 개발 팁

### API 직접 테스트

```bash
# 주문 생성
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000
  }'

# 통계 조회
curl http://localhost:8080/stats | jq

# 주문 목록
curl http://localhost:8080/orders?limit=10 | jq
```

### Kafka 메시지 확인

```bash
# orders 토픽 메시지 보기
docker exec -it kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 10

# order-stats 토픽 메시지 보기
docker exec -it kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --max-messages 10
```

### 코드 수정 후 재배포

**Server1 수정 시**:
```bash
cd infra-local
docker-compose stop server1
docker-compose build server1
docker-compose up -d server1
```

**Server2 수정 시**:
```bash
cd infra-local
docker-compose stop server2
docker-compose build server2
docker-compose up -d server2
```

### 실시간 모니터링 (4개 터미널)

```bash
# 터미널 1: Server1 로그
docker logs server1-app -f

# 터미널 2: Server2 로그
docker logs server2-streams -f

# 터미널 3: Kafka 로그
docker logs kafka-broker -f

# 터미널 4: 주문 생성
while true; do ./test-orders.sh 1; sleep 2; done
```

---

## 주요 URL

| 서비스 | URL | 설명 |
|--------|-----|------|
| 대시보드 | http://localhost:8080/dashboard | 실시간 통계 UI |
| 헬스체크 | http://localhost:8080/health | API 상태 |
| 통계 API | http://localhost:8080/stats | JSON 통계 |
| 주문 생성 | POST http://localhost:8080/orders | 주문 API |
| 주문 조회 | GET http://localhost:8080/orders | 주문 목록 |

---

## 다음 단계

- 📖 [상세 가이드](README.md#로컬-환경-실습) - 단계별 자세한 설명
- ☁️ [AWS 배포](README.md#aws-배포-가이드) - 실제 서버 배포
- 🔧 [Server1 코드](server1-app/README.md) - Node.js 코드 이해
- 🔧 [Server2 코드](server2-streams/README.md) - Java Kafka Streams 이해

---

**💡 TIP**: 처음 사용하시나요? `./setup-local.sh`로 자동 설정하세요!
