# Server1 - Order API & Dashboard

주문 수집, 데이터베이스 저장, Kafka 이벤트 발행 및 대시보드를 제공하는 서버입니다.

## 주요 기능

### 1. REST API

- **POST /orders**: 주문 생성
- **GET /orders**: 주문 목록 조회
- **GET /stats**: 실시간 통계 조회
- **GET /health**: 헬스 체크
- **GET /dashboard**: 대시보드 UI

### 2. 데이터베이스 연동

- PostgreSQL에 주문 데이터 저장
- 자동 스키마 초기화

### 3. Kafka 통합

- **Producer**: orders 토픽으로 주문 이벤트 발행
- **Consumer**: order-stats 토픽에서 통계 수신

## 동작 원리

```
1. 주문 생성 요청 수신 (POST /orders)
   ↓
2. PostgreSQL에 주문 저장
   ↓
3. Kafka Producer로 orders 토픽에 이벤트 발행
   ↓
4. Kafka Consumer가 order-stats 토픽에서 통계 수신
   ↓
5. 대시보드 API를 통해 실시간 통계 제공
```

이 흐름을 통해 **이벤트 기반 아키텍처**의 핵심인 "발행-구독" 패턴을 구현합니다.

## 로컬 개발 환경 설정

### 사전 요구사항

- Node.js 20 이상
- npm
- PostgreSQL (또는 Docker)
- Kafka 브로커 (서버2 또는 별도 실행)

### 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 수정
PORT=8080
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=orders_db
POSTGRES_USER=orders_user
POSTGRES_PASSWORD=orders_pass
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CLIENT_ID=server1-producer
```

### 실행

```bash
# 의존성 설치
npm install

# 개발 모드 실행
npm run dev

# 프로덕션 모드 실행
npm start
```

## Docker로 실행

### 이미지 빌드

```bash
docker build -t server1-app:latest .
```

### 컨테이너 실행

```bash
docker run -d \
  --name server1-app \
  -p 8080:8080 \
  -e POSTGRES_HOST=<postgres_host> \
  -e POSTGRES_DB=orders_db \
  -e POSTGRES_USER=orders_user \
  -e POSTGRES_PASSWORD=orders_pass \
  -e KAFKA_BOOTSTRAP_SERVERS=<kafka_broker>:9092 \
  server1-app:latest
```

## API 사용 예시

### 주문 생성

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000
  }'
```

**응답:**
```json
{
  "success": true,
  "order": {
    "order_id": "uuid-...",
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000,
    "status": "CREATED",
    "created_at": "2025-11-17T14:30:00.123Z"
  }
}
```

### 통계 조회

```bash
curl http://localhost:8080/stats
```

**응답:**
```json
{
  "success": true,
  "stats": {
    "totalOrders": 100,
    "totalSales": 2500000,
    "byRegion": {
      "Seoul": {
        "orders": 50,
        "sales": 1250000
      },
      "Busan": {
        "orders": 30,
        "sales": 750000
      }
    },
    "lastUpdated": "2025-11-17T14:30:00.123Z"
  }
}
```

### 대시보드 접속

브라우저에서 http://localhost:8080/dashboard 접속

## 디렉토리 구조

```
server1-app/
├── src/
│   ├── index.js              # 메인 애플리케이션
│   ├── db.js                 # PostgreSQL 연결 및 쿼리
│   ├── kafka-producer.js     # Kafka Producer
│   └── kafka-consumer.js     # Kafka Consumer
├── public/
│   └── dashboard.html        # 대시보드 UI
├── package.json              # 의존성 및 스크립트
├── Dockerfile                # Docker 이미지 빌드
├── .env.example              # 환경 변수 템플릿
└── README.md                 # 이 파일
```

## 트러블슈팅

### PostgreSQL 연결 실패

**문제:** `Error: connect ECONNREFUSED`

**해결:**
1. PostgreSQL이 실행 중인지 확인
2. POSTGRES_HOST가 올바른지 확인
3. 네트워크 연결 확인

```bash
# PostgreSQL 상태 확인
docker ps | grep postgres

# 연결 테스트
docker exec postgres-orders pg_isready -U orders_user
```

### Kafka 연결 실패

**문제:** `Error: Connection timeout`

**해결:**
1. Kafka 브로커가 실행 중인지 확인
2. KAFKA_BOOTSTRAP_SERVERS가 올바른지 확인
3. 네트워크/방화벽 설정 확인

```bash
# Kafka 상태 확인
docker logs kafka-broker --tail 50

# 연결 테스트
telnet <kafka_host> 9092
```

## 의존성

- **express**: Web framework
- **pg**: PostgreSQL client
- **kafkajs**: Kafka client
- **uuid**: UUID 생성
- **dotenv**: 환경 변수 관리
- **cors**: CORS 지원

## 라이센스

MIT
