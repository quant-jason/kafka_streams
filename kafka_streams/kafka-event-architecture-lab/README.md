# Kafka Event Architecture Lab

실시간 이벤트 기반 아키텍처 실습 프로젝트 - Kafka Streams를 활용한 주문 처리 및 통계 시스템

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [아키텍처 설명](#아키텍처-설명)
- [📚 Kafka Streams 핵심 개념](#-kafka-streams-핵심-개념) ⭐ **중요**
- [기술 스택](#기술-스택)
- [🏠 로컬 개발 환경에서 사용하기](#-로컬-개발-환경에서-사용하기) ⭐ **추천**
- [로컬 환경 실습 (상세)](#로컬-환경-실습)
- [AWS 배포 가이드](#aws-배포-가이드)
- [API 명세](#api-명세)
- [트러블슈팅](#트러블슈팅)

---

## 프로젝트 개요

이 프로젝트는 **이벤트 기반 아키텍처(Event-Driven Architecture)**의 핵심 개념을 실습하기 위한 완전한 데모 시스템입니다.

### 주요 기능

1. **주문 생성 및 관리** (Server1)
   - REST API를 통한 주문 생성
   - PostgreSQL에 주문 데이터 저장
   - Kafka로 주문 이벤트 발행
   - 웹 UI를 통한 랜덤 주문 생성 버튼 제공

2. **실시간 통계 처리** (Server2)
   - Kafka Streams를 이용한 스트림 처리
   - 주문 수 및 매출 실시간 집계
   - 지역별 통계 분석
   - Stateful 처리 (RocksDB 상태 저장소)

3. **대시보드** (Server1)
   - KStream/KTable 처리 흐름 시각화
   - 실시간 이벤트 스트림 모니터링
   - 집계 상태 변경 이력 추적
   - 통계 시각화 및 자동 갱신
   - 웹 UI 랜덤 주문 생성 버튼

### 학습 목표

- Kafka Producer/Consumer 패턴 이해
- Kafka Streams 스트림 처리 구현
- 이벤트 기반 마이크로서비스 간 통신
- Docker를 활용한 컨테이너 오케스트레이션
- AWS 환경에서의 실제 배포

---

## 아키텍처 설명

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                        사용자 / 클라이언트                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Server1 (8080)                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  REST API (Express.js)                                │  │
│  │  • POST /orders     - 주문 생성                        │  │
│  │  • GET /orders      - 주문 조회                        │  │
│  │  • GET /stats       - 통계 조회                        │  │
│  │  • GET /dashboard   - 대시보드 UI                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                              │                               │
│      ┌──────────────────────┼──────────────────────┐        │
│      ▼                      ▼                      ▼        │
│  ┌────────┐          ┌────────────┐         ┌──────────┐   │
│  │  DB    │          │   Kafka    │         │  Kafka   │   │
│  │ Writer │          │  Producer  │         │ Consumer │   │
│  └────────┘          └────────────┘         └──────────┘   │
└──────┬──────────────────────┬──────────────────────┬────────┘
       │                      │                      │
       ▼                      │                      │
┌────────────┐                │                      │
│ PostgreSQL │                │                      │
│  Database  │                │                      │
└────────────┘                │                      │
                              ▼                      │
                    ┌─────────────────────┐          │
                    │   Kafka Broker      │          │
                    │   (Port 9092)       │          │
                    │                     │          │
                    │  Topics:            │          │
                    │  • orders           │◄─────────┘
                    │  • order-stats      │
                    └─────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Server2                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Kafka Streams Application (Java)                     │  │
│  │                                                        │  │
│  │  1. orders 토픽 구독                                   │  │
│  │  2. 주문 데이터 집계:                                  │  │
│  │     • 총 주문 수                                       │  │
│  │     • 총 매출                                          │  │
│  │     • 지역별 통계                                      │  │
│  │  3. order-stats 토픽으로 결과 발행                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 데이터 흐름 (이벤트 아키텍처)

```
1. 주문 생성
   사용자 → POST /orders → Server1

2. 데이터 저장 및 이벤트 발행
   Server1 → PostgreSQL (주문 저장)
   Server1 → Kafka (orders 토픽) (이벤트 발행)

3. 스트림 처리
   Kafka (orders 토픽) → Server2 (Kafka Streams)
   Server2 → 실시간 집계 처리
   Server2 → Kafka (order-stats 토픽) (통계 발행)

4. 대시보드 갱신
   Kafka (order-stats 토픽) → Server1 (Consumer)
   Server1 → 대시보드 UI (실시간 갱신)
```

### 이벤트 스키마

#### orders 토픽 (Input)

```json
{
  "order_id": "uuid-1234-5678",
  "user_id": "user-001",
  "store_id": "store-777",
  "region": "Seoul",
  "price": 15000,
  "status": "CREATED",
  "created_at": "2025-11-17T13:45:00Z"
}
```

#### order-stats 토픽 (Output)

```json
{
  "totalOrders": 100,
  "totalSales": 1500000,
  "byRegion": {
    "Seoul": {
      "orders": 50,
      "sales": 750000
    },
    "Busan": {
      "orders": 30,
      "sales": 450000
    },
    "Incheon": {
      "orders": 20,
      "sales": 300000
    }
  },
  "lastUpdated": "2025-11-17T13:50:00Z"
}
```

---

## 📚 Kafka Streams 핵심 개념

이 프로젝트는 Kafka Streams의 핵심 개념들을 실제로 구현한 예제입니다.

### KStream이란?

**KStream**은 무한한 레코드의 **스트림(Stream)**입니다.

#### 특징
- **INSERT-ONLY**: 새로운 이벤트만 추가됨
- **Immutable**: 기존 데이터는 변경되지 않음
- **무한 스트림**: 끝이 없는 연속적인 데이터 흐름
- **이벤트 로그**: 각 레코드는 독립적인 사실(fact)

#### 예시
```
주문 이벤트 스트림:
order-1: {user_id: "user-001", price: 15000} ← 15:00:01
order-2: {user_id: "user-002", price: 25000} ← 15:00:05
order-3: {user_id: "user-001", price: 10000} ← 15:00:10
...계속 추가됨
```

#### 이 프로젝트에서의 사용
```java
// orders 토픽에서 KStream 생성
KStream<String, Order> ordersStream = builder.stream(
    "orders",
    Consumed.with(Serdes.String(), orderSerde)
);

// 각 주문 이벤트는 독립적으로 처리됨
ordersStream.foreach((key, order) -> {
    logger.info("📥 Received order: {}", order.getOrderId());
});
```

### KTable이란?

**KTable**은 변경 가능한 **상태 테이블(State Table)**입니다.

#### 특징
- **UPDATE 가능**: 같은 키의 값이 업데이트됨
- **Mutable**: 최신 값으로 덮어씀
- **Changelog Stream**: 변경 이력을 추적
- **현재 상태**: 각 키에 대한 최신 값만 유지

#### 예시
```
통계 테이블 (Key: "global"):
15:00:01 → {totalOrders: 1, totalSales: 15000}
15:00:05 → {totalOrders: 2, totalSales: 40000}  ← 업데이트됨
15:00:10 → {totalOrders: 3, totalSales: 50000}  ← 업데이트됨
```

#### 이 프로젝트에서의 사용
```java
// KStream을 집계하여 KTable 생성
KTable<String, OrderStats> statsTable = ordersStream
    .filter((key, order) -> order != null)
    .groupBy((key, order) -> "global")
    .aggregate(
        OrderStats::new,  // 초기값
        (key, order, stats) -> {
            // 새 주문마다 통계 업데이트
            stats.setTotalOrders(stats.getTotalOrders() + 1);
            stats.setTotalSales(stats.getTotalSales() + order.getPrice());
            return stats;
        },
        Materialized.with(Serdes.String(), statsSerde)
    );
```

### KStream vs KTable 비교

| 구분 | KStream | KTable |
|-----|---------|--------|
| **데이터 모델** | 이벤트 스트림 | 상태 테이블 |
| **연산 타입** | INSERT only | INSERT, UPDATE, DELETE |
| **저장 방식** | 모든 이벤트 보관 | 최신 값만 보관 |
| **사용 사례** | 주문 내역, 클릭 로그 | 사용자 프로필, 재고 수량 |
| **DB 비유** | Transaction Log | Table |
| **예시** | "주문이 생성됨" | "현재 재고는 10개" |

### Stateless vs Stateful 연산

#### Stateless 연산 (무상태)
이전 데이터를 기억할 필요가 없는 연산

```java
// filter: 조건에 맞는 레코드만 통과
ordersStream.filter((key, order) -> order.getPrice() > 10000)

// map: 각 레코드를 변환
ordersStream.map((key, order) ->
    KeyValue.pair(order.getRegion(), order)
)

// flatMap: 하나의 레코드를 여러 개로 변환
ordersStream.flatMap((key, order) ->
    Arrays.asList(
        KeyValue.pair(order.getUserId(), order),
        KeyValue.pair(order.getStoreId(), order)
    )
)
```

#### Stateful 연산 (상태 유지)
이전 데이터를 기억하고 상태를 유지하는 연산

```java
// groupBy: 키 기준으로 그룹화 (리파티셔닝)
ordersStream.groupBy((key, order) -> order.getRegion())

// aggregate: 집계 연산 (KTable 생성)
.aggregate(
    () -> new RegionStats(),      // 초기값
    (key, order, stats) -> {      // Aggregator
        stats.increment();
        return stats;
    },
    Materialized.with(...)         // 상태 저장소 설정
)

// reduce: 값을 축소 (KTable 생성)
.reduce((oldValue, newValue) -> oldValue + newValue)

// count: 개수 세기 (KTable 생성)
.count()
```

### 주요 DSL 연산자

#### 1. filter / filterNot
조건에 맞는/맞지 않는 레코드만 통과
```java
ordersStream
    .filter((key, order) -> order != null)
    .filterNot((key, order) -> order.getPrice() == 0)
```

#### 2. map / mapValues
레코드를 변환 (키-값 쌍 변환 / 값만 변환)
```java
// map: 키와 값 모두 변경
ordersStream.map((key, order) ->
    KeyValue.pair(order.getRegion(), order.getPrice())
)

// mapValues: 값만 변경 (키는 유지)
ordersStream.mapValues(order -> order.getPrice())
```

#### 3. groupBy / groupByKey
데이터를 그룹화
```java
// groupBy: 새로운 키로 그룹화 (리파티셔닝 발생)
ordersStream.groupBy((key, order) -> order.getRegion())

// groupByKey: 현재 키로 그룹화 (리파티셔닝 없음)
ordersStream.groupByKey()
```

#### 4. aggregate
집계 연산 수행
```java
ordersStream
    .groupBy((key, order) -> order.getRegion())
    .aggregate(
        RegionStats::new,              // Initializer
        (key, order, stats) -> {       // Aggregator
            stats.addOrder(order);
            return stats;
        },
        Materialized.<String, RegionStats, KeyValueStore<Bytes, byte[]>>as("region-stats-store")
            .withKeySerde(Serdes.String())
            .withValueSerde(regionStatsSerde)
    )
```

#### 5. join
두 스트림/테이블을 조인
```java
// KStream-KTable join
ordersStream.join(
    usersTable,                        // KTable
    (order, user) -> {                 // ValueJoiner
        order.setUserName(user.getName());
        return order;
    },
    Joined.with(Serdes.String(), orderSerde, userSerde)
)
```

#### 6. peek
레코드를 변경하지 않고 부수 효과만 수행 (디버깅용)
```java
ordersStream
    .peek((key, order) -> logger.info("Processing: {}", order))
    .filter(...)
    .peek((key, order) -> logger.info("After filter: {}", order))
```

#### 7. to
결과를 토픽으로 전송
```java
statsTable.toStream()
    .to("order-stats", Produced.with(Serdes.String(), statsSerde))
```

### 이 프로젝트의 실제 구현

#### 전체 토폴로지
```java
KStream<String, Order> ordersStream = builder.stream("orders")
    .filter((key, order) -> order != null)           // ① Stateless
    .groupBy((key, order) -> "global")               // ② Stateful (그룹화)
    .aggregate(                                       // ③ Stateful (집계)
        OrderStats::new,
        (key, order, stats) -> {
            stats.setTotalOrders(stats.getTotalOrders() + 1);
            stats.setTotalSales(stats.getTotalSales() + order.getPrice());

            // 지역별 통계도 업데이트
            Map<String, RegionStats> byRegion = stats.getByRegion();
            RegionStats regionStats = byRegion.getOrDefault(
                order.getRegion(),
                new RegionStats(0, 0.0)
            );
            regionStats.setOrders(regionStats.getOrders() + 1);
            regionStats.setSales(regionStats.getSales() + order.getPrice());
            byRegion.put(order.getRegion(), regionStats);

            return stats;
        },
        Materialized.with(Serdes.String(), statsSerde) // RocksDB에 상태 저장
    );

statsTable.toStream()
    .to("order-stats", Produced.with(...));           // ④ 결과 발행
```

#### 처리 흐름
```
1. KStream (orders 토픽)
   ↓ filter (null 제거)
2. Stateless 처리
   ↓ groupBy (키 = "global")
3. Grouped KStream
   ↓ aggregate (집계)
4. KTable (상태 저장)
   ↓ toStream (스트림 변환)
5. KStream (order-stats 토픽으로 발행)
```

### Materialized: 상태 저장소

**Materialized**는 상태를 저장하는 방법을 지정합니다.

```java
Materialized.<String, OrderStats, KeyValueStore<Bytes, byte[]>>as("order-stats-store")
    .withKeySerde(Serdes.String())
    .withValueSerde(statsSerde)
    .withCachingEnabled()           // 캐싱 활성화
    .withLoggingEnabled(...)        // Changelog 활성화
```

#### 특징
- **영구 저장**: RocksDB에 상태 저장
- **Fault Tolerance**: Changelog 토픽으로 백업
- **빠른 조회**: 로컬 상태 저장소에서 즉시 조회
- **재시작 복구**: 애플리케이션 재시작 시 상태 복원

#### Changelog 토픽
Kafka Streams는 자동으로 상태 변경 이력을 저장하는 토픽을 생성합니다:
```
order-stats-app-KSTREAM-AGGREGATE-STATE-STORE-0000000004-changelog
```

이 토픽은:
- 상태 저장소의 모든 변경사항 기록
- 애플리케이션 재시작 시 상태 복구에 사용
- Compacted topic (최신 값만 유지)

### 실습: 대시보드에서 확인하기

http://localhost:8080/dashboard 에서 실시간으로 확인:

1. **🎲 랜덤 주문 생성** 버튼 클릭
2. **KStream 패널**: 주문 이벤트가 스트림으로 추가됨 (INSERT only)
3. **처리 흐름도**: Producer → KStream → GroupBy → Aggregate → KTable → Output
4. **KTable 패널**: 집계 상태가 업데이트됨 (UPDATE)
5. **통계 카드**: 총 주문수, 총 매출, 지역별 통계 갱신

---

## 기술 스택

### Server1 (주문 API & 대시보드)
- **언어**: Node.js 20
- **프레임워크**: Express.js
- **데이터베이스**: PostgreSQL 16
- **Kafka 클라이언트**: KafkaJS
- **기타**: UUID, dotenv, CORS

### Server2 (Kafka Streams)
- **언어**: Java 17
- **프레임워크**: Kafka Streams 3.6.1
- **빌드 도구**: Gradle 8.5
- **JSON 처리**: Jackson
- **로깅**: SLF4J

### 인프라
- **컨테이너**: Docker & Docker Compose
- **Kafka**: Confluent Platform 7.5.0
- **Zookeeper**: Confluent Zookeeper 7.5.0
- **클라우드**: AWS (EC2, Security Groups)

---

## 🏠 로컬 개발 환경에서 사용하기

> **💡 빠른 시작**: 이 섹션은 로컬 PC에서 전체 시스템을 실행하는 가장 간단한 방법입니다.

### 필수 요구사항

- **Docker Desktop** 설치 ([다운로드](https://www.docker.com/products/docker-desktop))
- **Git** 설치
- 최소 8GB RAM, 20GB 여유 디스크 공간

### 3분 안에 시작하기

#### 🎯 방법 1: 자동 설정 (가장 간단!) ⭐

```bash
# 1. 프로젝트 다운로드
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab

# 2. 자동 설정 스크립트 실행
./setup-local.sh
```

이 스크립트가 자동으로 수행하는 작업:
- ✅ Docker 설치 확인
- ✅ 전체 시스템 시작 (Docker Compose)
- ✅ Kafka 토픽 생성 (orders, order-stats)
- ✅ Server2 재시작
- ✅ 헬스 체크

완료되면 바로 `http://localhost:8080/dashboard`로 접속하여 대시보드를 확인할 수 있습니다!

---

#### 🔧 방법 2: 수동 설정 (단계별)

#### 1️⃣ 프로젝트 다운로드

```bash
# Git으로 클론
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab

# 또는 ZIP 다운로드 후 압축 해제
```

#### 2️⃣ 시스템 실행

```bash
# Docker Compose로 전체 시스템 시작
cd infra-local
docker-compose up -d --build
```

⏱️ **초기 빌드 시간**: 3-5분 (이미지 다운로드 + 빌드)
⏱️ **이후 실행 시간**: 30초

**실행되는 서비스:**
- ✅ Zookeeper (Kafka 코디네이터)
- ✅ Kafka Broker (메시지 브로커)
- ✅ PostgreSQL (주문 데이터베이스)
- ✅ Server1 (주문 API + 대시보드)
- ✅ Server2 (Kafka Streams 통계 처리)

#### 3️⃣ Kafka 토픽 생성 (최초 1회)

```bash
# orders 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic orders \
  --partitions 3 --replication-factor 1 \
  --if-not-exists

# order-stats 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-stats \
  --partitions 3 --replication-factor 1 \
  --if-not-exists

# Server2 재시작 (토픽 인식)
docker restart server2-streams
```

> **💡 TIP**: 토픽은 최초 1회만 생성하면 됩니다. 이후에는 자동으로 유지됩니다.

#### 4️⃣ 동작 확인

```bash
# 프로젝트 루트로 이동
cd ..

# 헬스 체크
./check-health.sh
```

**정상 출력 예시:**
```
🏥 Checking service health...

1️⃣  Server1 (Order API):
   ✅ Healthy

2️⃣  PostgreSQL:
   ✅ Healthy

3️⃣  Kafka Broker:
   ✅ Healthy

4️⃣  Zookeeper:
   ✅ Healthy

5️⃣  Server2 (Kafka Streams):
   ✅ Running
```

#### 5️⃣ 테스트 실행

**방법 1: 웹 대시보드에서 주문 생성** ⭐ (가장 쉬움!)

브라우저에서 `http://localhost:8080/dashboard` 접속 후:
- **🎲 랜덤 주문 생성** 버튼 클릭 → 즉시 랜덤 주문 생성!
- 또는 폼에 직접 입력하여 **주문 생성** 버튼 클릭

실시간으로 다음을 확인 가능:
- 처리 흐름도 애니메이션 (Producer → KStream → ... → Output)
- KStream 패널에 새 이벤트 표시
- KTable 패널에 집계 상태 업데이트
- 통계 즉시 갱신

**방법 2: 쉘 스크립트로 대량 생성**

```bash
# 10개의 샘플 주문 생성
./test-orders.sh 10
```

**출력 예시:**
```
📦 Creating 10 sample orders...
✅ Order 1 created: abc123... | Seoul | ₩25,000
✅ Order 2 created: def456... | Busan | ₩18,000
...
✅ Created 10 orders
📊 Check dashboard at: http://localhost:8080/dashboard
```

**방법 3: API 직접 호출**

아래 "API 직접 테스트" 섹션 참조

#### 6️⃣ 대시보드 확인

브라우저를 열고 다음 URL에 접속:

```
http://localhost:8080/dashboard
```

**대시보드에서 확인 가능한 항목:**
- 🌊 **Kafka Streams 처리 흐름도**: Producer → KStream → GroupBy → Aggregate → KTable → Output
- 📥 **KStream 실시간 이벤트**: 주문 생성 이벤트 스트림 (INSERT only)
- 📊 **KTable 집계 상태**: 상태 변경 이력 추적 (UPDATE 가능)
- 🎲 **랜덤 주문 생성 버튼**: 클릭 한 번으로 즉시 랜덤 주문 생성
- 📝 **수동 주문 입력 폼**: 고객ID, 상점ID, 지역, 가격 직접 입력
- 📦 **총 주문 수**: KTable에서 집계된 총 주문 건수
- 💰 **총 매출**: KTable에서 집계된 총 매출액
- 📍 **지역별 통계**: 서울, 부산, 인천, 대구, 광주 지역별 주문 수 및 매출
- 🔄 **자동 갱신**: 3초마다 자동 업데이트

### 일상적인 사용 방법

#### 시스템 시작

```bash
cd infra-local
docker-compose up -d
```

#### 시스템 종료

```bash
cd infra-local
docker-compose down
```

#### 완전 초기화 (데이터 포함)

```bash
cd infra-local
docker-compose down -v
```

#### 로그 확인

```bash
# 전체 로그 (실시간)
docker-compose logs -f

# Server1 로그만
docker logs server1-app -f

# Server2 로그만
docker logs server2-streams -f

# Kafka 로그만
docker logs kafka-broker -f
```

#### 특정 서비스만 재시작

```bash
docker restart server1-app      # Server1만 재시작
docker restart server2-streams  # Server2만 재시작
docker restart kafka-broker     # Kafka만 재시작
```

### API 직접 테스트

#### 주문 생성

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

#### 통계 조회

```bash
# 스크립트 사용
./check-stats.sh

# 또는 curl 직접 사용
curl http://localhost:8080/stats | jq
```

**응답:**
```json
{
  "success": true,
  "stats": {
    "totalOrders": 100,
    "totalSales": 2500000,
    "byRegion": {
      "Seoul": {"orders": 50, "sales": 1250000},
      "Busan": {"orders": 30, "sales": 750000},
      "Incheon": {"orders": 20, "sales": 500000}
    },
    "lastUpdated": "2025-11-17T14:30:00.123Z"
  }
}
```

#### 주문 목록 조회

```bash
curl http://localhost:8080/orders?limit=10
```

### 문제 해결

#### 포트가 이미 사용 중

**증상:**
```
Error: Ports are not available: port is already allocated
```

**해결:**
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8080  # Server1
lsof -i :9092  # Kafka
lsof -i :5432  # PostgreSQL

# 프로세스 종료 또는 Docker Compose에서 포트 변경
```

#### Server2가 계속 재시작됨

**원인**: Kafka 토픽이 생성되지 않음

**해결:**
```bash
# 토픽 확인
docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list

# 토픽이 없으면 3단계로 돌아가서 토픽 생성
```

#### 통계가 0으로 표시됨

**해결:**
```bash
# 1. Server2 로그 확인
docker logs server2-streams --tail 50

# 2. Server2 재시작
docker restart server2-streams

# 3. 새 주문 생성
./test-orders.sh 5

# 4. 통계 다시 확인
./check-stats.sh
```

#### Docker 메모리 부족

**증상:**
```
Error: docker: Error response from daemon: OOM command not allowed
```

**해결:**
1. Docker Desktop 설정 열기
2. Resources → Memory 설정을 8GB 이상으로 증가
3. Apply & Restart

### 개발 팁

#### 코드 수정 후 재배포

**Server1 (Node.js) 수정 시:**
```bash
cd infra-local
docker-compose stop server1
docker-compose build server1
docker-compose up -d server1
```

**Server2 (Java) 수정 시:**
```bash
cd infra-local
docker-compose stop server2
docker-compose build server2
docker-compose up -d server2
```

#### 실시간 로그 모니터링

```bash
# 터미널을 4개로 분할하여 각각 모니터링
# 터미널 1
docker logs server1-app -f

# 터미널 2
docker logs server2-streams -f

# 터미널 3
docker logs kafka-broker -f

# 터미널 4
./test-orders.sh 1  # 반복 실행
```

#### Kafka 메시지 직접 확인

```bash
# orders 토픽 메시지 확인
docker exec -it kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 10

# order-stats 토픽 메시지 확인
docker exec -it kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --max-messages 10
```

### 주요 엔드포인트

| 서비스 | URL | 설명 |
|--------|-----|------|
| 대시보드 | http://localhost:8080/dashboard | 실시간 통계 대시보드 |
| 헬스체크 | http://localhost:8080/health | API 상태 확인 |
| 주문 생성 | POST http://localhost:8080/orders | 새 주문 생성 |
| 주문 조회 | GET http://localhost:8080/orders | 주문 목록 |
| 통계 조회 | GET http://localhost:8080/stats | 실시간 통계 |

### 성능 튜닝 (선택사항)

#### Kafka 파티션 증가

```bash
# 처리량 증가를 위해 파티션 수 증가
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --alter --topic orders \
  --partitions 6
```

#### 대시보드 갱신 주기 변경

`server1-app/public/dashboard.html` 파일 수정:
```javascript
// 5초 → 2초로 변경
const REFRESH_INTERVAL = 2000;
```

### 다음 단계

- 📖 **상세 가이드**: [로컬 환경 실습 (상세)](#로컬-환경-실습) - 단계별 자세한 설명
- ☁️ **AWS 배포**: [AWS 배포 가이드](#aws-배포-가이드) - 실제 서버에 배포
- 🔧 **코드 이해**: `server1-app/README.md`, `server2-streams/README.md` 참고

---

## 로컬 환경 실습 (상세)

> **💡 참고**: 빠르게 시작하려면 [로컬 개발 환경에서 사용하기](#-로컬-개발-환경에서-사용하기)를 먼저 보세요.

로컬 환경에서 전체 시스템을 Docker Compose로 실행하고 테스트하는 상세 가이드입니다.

### 사전 요구사항

다음 소프트웨어가 설치되어 있어야 합니다:

```bash
# Docker 버전 확인 (20.10 이상)
docker --version

# Docker Compose 버전 확인 (2.0 이상)
docker-compose --version

# (선택) curl - API 테스트용
curl --version

# (선택) jq - JSON 파싱용
jq --version
```

### Step 1: 프로젝트 클론

```bash
# 리포지토리 클론
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab

# 디렉토리 구조 확인
ls -la
```

**예상 출력:**
```
drwxr-xr-x  server1-app/         # 주문 API & 대시보드
drwxr-xr-x  server2-streams/     # Kafka Streams 처리
drwxr-xr-x  infra-local/         # Docker Compose 설정
-rwxr-xr-x  test-orders.sh       # 주문 생성 테스트 스크립트
-rwxr-xr-x  check-stats.sh       # 통계 확인 스크립트
-rwxr-xr-x  check-health.sh      # 헬스 체크 스크립트
-rw-r--r--  README.md
```

### Step 2: 전체 시스템 실행

Docker Compose로 모든 서비스를 한 번에 시작합니다.

```bash
cd infra-local

# 모든 서비스 빌드 및 시작 (백그라운드)
docker-compose up -d --build
```

**실행 과정:**
1. Zookeeper 컨테이너 시작
2. Kafka 브로커 컨테이너 시작
3. PostgreSQL 데이터베이스 시작
4. Server1 이미지 빌드 및 시작
5. Server2 이미지 빌드 및 시작

**예상 소요 시간:** 초기 빌드 3-5분, 이후 실행 30초~1분

```bash
# 실행 중인 컨테이너 확인
docker-compose ps
```

**예상 출력:**
```
NAME               IMAGE                    STATUS        PORTS
kafka-broker       confluentinc/cp-kafka    Up (healthy)  0.0.0.0:9092->9092/tcp
postgres-orders    postgres:16-alpine       Up (healthy)  0.0.0.0:5432->5432/tcp
server1-app        infra-local-server1      Up            0.0.0.0:8080->8080/tcp
server2-streams    infra-local-server2      Up
zookeeper          confluentinc/cp-zookeeper Up (healthy) 0.0.0.0:2181->2181/tcp
```

### Step 3: 서비스 헬스 체크

모든 서비스가 정상적으로 실행되고 있는지 확인합니다.

```bash
# 프로젝트 루트로 이동
cd ..

# 헬스 체크 스크립트 실행
./check-health.sh
```

**예상 출력:**
```
🏥 Checking service health...

1️⃣  Server1 (Order API):
   ✅ Healthy

2️⃣  PostgreSQL:
   ✅ Healthy

3️⃣  Kafka Broker:
   ✅ Healthy

4️⃣  Zookeeper:
   ✅ Healthy

5️⃣  Server2 (Kafka Streams):
   ✅ Running

📋 Summary:
NAMES              STATUS          PORTS
kafka-broker       Up 2 minutes    0.0.0.0:9092->9092/tcp
postgres-orders    Up 2 minutes    0.0.0.0:5432->5432/tcp
server1-app        Up 1 minute     0.0.0.0:8080->8080/tcp
server2-streams    Up 1 minute
zookeeper          Up 2 minutes    0.0.0.0:2181->2181/tcp
```

### Step 4: 로그 확인

각 서비스의 로그를 확인하여 정상 작동 여부를 검증합니다.

```bash
# Server1 로그 확인
docker logs server1-app --tail 50

# Server2 로그 확인
docker logs server2-streams --tail 50

# Kafka 로그 확인
docker logs kafka-broker --tail 50

# 전체 로그 실시간 모니터링
cd infra-local
docker-compose logs -f
```

**Server1 정상 로그 예시:**
```
✅ Database schema initialized
✅ Kafka Producer connected
✅ Kafka Consumer connected and subscribed to order-stats
🚀 Server1 running on http://localhost:8080
📊 Dashboard: http://localhost:8080/dashboard
```

**Server2 정상 로그 예시:**
```
Starting Kafka Streams application...
Input topic: orders
Output topic: order-stats
✅ Topology built successfully
✅ Kafka Streams application started successfully
```

### Step 5: 대시보드 확인

웹 브라우저를 열고 대시보드에 접속합니다.

```bash
# 브라우저에서 열기
open http://localhost:8080/dashboard
# 또는
# Windows: start http://localhost:8080/dashboard
# Linux: xdg-open http://localhost:8080/dashboard
```

**대시보드 화면:**
- 총 주문 수
- 총 매출
- 지역별 통계
- 최근 주문 내역

### Step 6: 주문 생성 테스트

#### 방법 1: 테스트 스크립트 사용 (권장)

```bash
# 10개의 샘플 주문 생성
./test-orders.sh 10
```

**예상 출력:**
```
📦 Creating 10 sample orders...
🔗 Server URL: http://localhost:8080

✅ Order 1 created: a1b2c3d4-... | Seoul | ₩25000
✅ Order 2 created: e5f6g7h8-... | Busan | ₩18000
✅ Order 3 created: i9j0k1l2-... | Incheon | ₩32000
...

✅ Created 10 orders
📊 Check dashboard at: http://localhost:8080/dashboard
```

#### 방법 2: curl로 수동 생성

```bash
# 단일 주문 생성
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000
  }'
```

**성공 응답:**
```json
{
  "success": true,
  "order": {
    "order_id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000,
    "status": "CREATED",
    "created_at": "2025-11-17T14:30:00.123Z"
  }
}
```

#### 방법 3: 대시보드 UI에서 생성

1. http://localhost:8080/dashboard 접속
2. "새 주문 생성" 폼 작성
3. "주문 생성" 버튼 클릭
4. 실시간으로 통계가 갱신되는 것 확인

### Step 7: 통계 확인

```bash
# 현재 통계 조회
./check-stats.sh
```

**예상 출력:**
```
📊 Fetching current statistics from http://localhost:8080/stats...

Response:
{
  "success": true,
  "stats": {
    "totalOrders": 10,
    "totalSales": 245000,
    "byRegion": {
      "Seoul": {
        "orders": 4,
        "sales": 98000
      },
      "Busan": {
        "orders": 3,
        "sales": 72000
      },
      "Incheon": {
        "orders": 3,
        "sales": 75000
      }
    },
    "lastUpdated": "2025-11-17T14:30:15.456Z"
  }
}

📊 Dashboard URL: http://localhost:8080/dashboard
```

### Step 8: Kafka 토픽 확인 (고급)

Kafka 내부의 메시지를 직접 확인할 수 있습니다.

```bash
# Kafka 컨테이너 내부로 진입
docker exec -it kafka-broker bash

# orders 토픽 메시지 확인
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 5

# order-stats 토픽 메시지 확인
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --max-messages 5

# 토픽 리스트 확인
kafka-topics --bootstrap-server localhost:9092 --list

# 토픽 상세 정보 확인
kafka-topics --bootstrap-server localhost:9092 \
  --describe \
  --topic orders

# 컨테이너에서 나가기
exit
```

### Step 9: 부하 테스트

대량의 주문을 생성하여 시스템의 실시간 처리 능력을 확인합니다.

```bash
# 100개의 주문 생성
./test-orders.sh 100

# 대시보드에서 실시간으로 통계가 갱신되는지 확인
open http://localhost:8080/dashboard
```

### Step 10: 시스템 종료

```bash
cd infra-local

# 모든 컨테이너 정지 및 삭제
docker-compose down

# 볼륨까지 삭제 (데이터 완전 초기화)
docker-compose down -v

# 이미지까지 삭제
docker-compose down -v --rmi all
```

---

## AWS 배포 가이드

실제 2대의 AWS EC2 인스턴스에 배포하는 전체 과정입니다.

### 아키텍처 구성

```
┌─────────────────────────────────────────────┐
│              AWS VPC                         │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │  EC2 Instance 1 (Server1)              │ │
│  │  Ubuntu 22.04 LTS                      │ │
│  │  • PostgreSQL Container                │ │
│  │  • Server1 Application Container       │ │
│  │  Public IP: <SERVER1_IP>               │ │
│  │  Port: 8080 (HTTP)                     │ │
│  └────────────────────────────────────────┘ │
│                     │                        │
│                     │ Kafka                  │
│                     ▼ (9092)                 │
│  ┌────────────────────────────────────────┐ │
│  │  EC2 Instance 2 (Server2)              │ │
│  │  Ubuntu 22.04 LTS                      │ │
│  │  • Zookeeper Container                 │ │
│  │  • Kafka Broker Container              │ │
│  │  • Server2 Streams Container           │ │
│  │  Public IP: <SERVER2_IP>               │ │
│  │  Port: 9092 (Kafka)                    │ │
│  └────────────────────────────────────────┘ │
│                                              │
└─────────────────────────────────────────────┘
```

### 사전 준비

#### 1. AWS EC2 인스턴스 생성

**Server1용 EC2:**
- AMI: Ubuntu 22.04 LTS
- 인스턴스 타입: t3.medium (2 vCPU, 4GB RAM) 이상
- 스토리지: 30GB GP3
- 보안 그룹:
  - SSH (22): 내 IP에서만
  - HTTP (8080): 0.0.0.0/0

**Server2용 EC2:**
- AMI: Ubuntu 22.04 LTS
- 인스턴스 타입: t3.medium (2 vCPU, 4GB RAM) 이상
- 스토리지: 30GB GP3
- 보안 그룹:
  - SSH (22): 내 IP에서만
  - Kafka (9092): Server1 보안 그룹

```bash
# AWS CLI로 보안 그룹 생성 예시

# Server1 보안 그룹
aws ec2 create-security-group \
  --group-name kafka-lab-server1-sg \
  --description "Security group for Server1"

aws ec2 authorize-security-group-ingress \
  --group-id <SERVER1_SG_ID> \
  --protocol tcp \
  --port 22 \
  --cidr <YOUR_IP>/32

aws ec2 authorize-security-group-ingress \
  --group-id <SERVER1_SG_ID> \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Server2 보안 그룹
aws ec2 create-security-group \
  --group-name kafka-lab-server2-sg \
  --description "Security group for Server2"

aws ec2 authorize-security-group-ingress \
  --group-id <SERVER2_SG_ID> \
  --protocol tcp \
  --port 22 \
  --cidr <YOUR_IP>/32

aws ec2 authorize-security-group-ingress \
  --group-id <SERVER2_SG_ID> \
  --protocol tcp \
  --port 9092 \
  --source-group <SERVER1_SG_ID>
```

#### 2. 키페어 설정

```bash
# 키페어 권한 설정
chmod 400 ~/your-keypair.pem
```

### Server2 배포 (Kafka 브로커 + Streams)

Server2를 먼저 배포하여 Kafka 인프라를 구축합니다.

#### Step 1: Server2 접속

```bash
# Server2 EC2에 SSH 접속
ssh -i ~/your-keypair.pem ubuntu@<SERVER2_PUBLIC_IP>
```

#### Step 2: Docker 설치

```bash
# 시스템 패키지 업데이트
sudo apt-get update
sudo apt-get upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 로그아웃 후 재접속하여 권한 적용
exit
ssh -i ~/your-keypair.pem ubuntu@<SERVER2_PUBLIC_IP>

# Docker 설치 확인
docker --version
docker-compose --version
```

#### Step 3: 프로젝트 클론 및 환경 설정

```bash
# Git 설치
sudo apt-get install -y git

# 프로젝트 클론
cd ~
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab/server2-streams
```

#### Step 4: Kafka 네트워크 생성

```bash
# Docker 네트워크 생성
docker network create kafka-network
```

#### Step 5: Zookeeper 실행

```bash
# Zookeeper 컨테이너 실행
docker run -d \
  --name zookeeper \
  --network kafka-network \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  -p 2181:2181 \
  confluentinc/cp-zookeeper:7.5.0

# Zookeeper 로그 확인
docker logs zookeeper --tail 20

# Zookeeper 상태 확인
docker exec zookeeper zkServer.sh status
```

#### Step 6: Kafka 브로커 실행

```bash
# 현재 인스턴스의 Private IP 확인
PRIVATE_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

echo "Private IP: $PRIVATE_IP"
echo "Public IP: $PUBLIC_IP"

# Kafka 브로커 컨테이너 실행
docker run -d \
  --name kafka-broker \
  --network kafka-network \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://$PRIVATE_IP:9092,PLAINTEXT_HOST://$PUBLIC_IP:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -e KAFKA_LOG_RETENTION_HOURS=168 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0

# Kafka 로그 확인 (정상 시작까지 약 30초 소요)
docker logs kafka-broker -f
# Ctrl+C로 로그 모니터링 종료

# Kafka 정상 작동 확인
docker exec kafka-broker kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

#### Step 7: Kafka 토픽 생성

```bash
# orders 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders \
  --partitions 3 \
  --replication-factor 1

# order-stats 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic order-stats \
  --partitions 3 \
  --replication-factor 1

# 토픽 생성 확인
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# 토픽 상세 정보 확인
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders
```

#### Step 8: Server2 Streams 애플리케이션 빌드 및 실행

```bash
cd ~/kafka-event-architecture-lab/server2-streams

# Docker 이미지 빌드
docker build -t server2-streams:latest .

# 빌드 확인
docker images | grep server2-streams

# Server2 애플리케이션 실행
docker run -d \
  --name server2-streams \
  --network kafka-network \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka-broker:9092 \
  -e APPLICATION_ID=order-stats-app \
  --restart unless-stopped \
  server2-streams:latest

# 로그 확인
docker logs server2-streams -f
# Ctrl+C로 종료
```

**정상 실행 로그 예시:**
```
Starting Kafka Streams application...
Input topic: orders
Output topic: order-stats
✅ Topology built successfully
✅ Kafka Streams application started successfully
```

#### Step 9: Server2 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps

# 전체 상태 확인
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**예상 출력:**
```
NAMES            STATUS              PORTS
server2-streams  Up 1 minute
kafka-broker     Up 3 minutes        0.0.0.0:9092->9092/tcp
zookeeper        Up 4 minutes        0.0.0.0:2181->2181/tcp
```

### Server1 배포 (주문 API + 대시보드)

#### Step 1: Server1 접속

로컬 터미널에서 새 세션을 열어 Server1에 접속합니다.

```bash
# Server1 EC2에 SSH 접속
ssh -i ~/your-keypair.pem ubuntu@<SERVER1_PUBLIC_IP>
```

#### Step 2: Docker 설치

```bash
# 시스템 패키지 업데이트
sudo apt-get update
sudo apt-get upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 로그아웃 후 재접속
exit
ssh -i ~/your-keypair.pem ubuntu@<SERVER1_PUBLIC_IP>

# Docker 설치 확인
docker --version
docker-compose --version
```

#### Step 3: 프로젝트 클론

```bash
# Git 설치
sudo apt-get install -y git

# 프로젝트 클론
cd ~
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab/server1-app
```

#### Step 4: PostgreSQL 실행

```bash
# Docker 네트워크 생성
docker network create app-network

# PostgreSQL 컨테이너 실행
docker run -d \
  --name postgres-orders \
  --network app-network \
  -e POSTGRES_USER=orders_user \
  -e POSTGRES_PASSWORD=orders_pass \
  -e POSTGRES_DB=orders_db \
  -p 5432:5432 \
  -v postgres-data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:16-alpine

# PostgreSQL 로그 확인
docker logs postgres-orders --tail 20

# PostgreSQL 연결 테스트
docker exec postgres-orders pg_isready -U orders_user -d orders_db
```

#### Step 5: Server1 환경 변수 설정

```bash
cd ~/kafka-event-architecture-lab/server1-app

# .env 파일 생성
cat > .env << EOF
PORT=8080
POSTGRES_HOST=postgres-orders
POSTGRES_PORT=5432
POSTGRES_DB=orders_db
POSTGRES_USER=orders_user
POSTGRES_PASSWORD=orders_pass
KAFKA_BOOTSTRAP_SERVERS=<SERVER2_PRIVATE_IP>:9092
KAFKA_CLIENT_ID=server1-producer
EOF

# Server2의 Private IP 확인 방법
# Server2 터미널에서: curl -s http://169.254.169.254/latest/meta-data/local-ipv4

# .env 파일 확인
cat .env
```

#### Step 6: Server1 애플리케이션 빌드 및 실행

```bash
# Docker 이미지 빌드
docker build -t server1-app:latest .

# 빌드 확인
docker images | grep server1-app

# Server1 애플리케이션 실행
docker run -d \
  --name server1-app \
  --network app-network \
  --link postgres-orders \
  -p 8080:8080 \
  --env-file .env \
  --restart unless-stopped \
  server1-app:latest

# 로그 확인
docker logs server1-app -f
# Ctrl+C로 종료
```

**정상 실행 로그 예시:**
```
✅ Database schema initialized
✅ Kafka Producer connected
✅ Kafka Consumer connected and subscribed to order-stats
🚀 Server1 running on http://localhost:8080
📊 Dashboard: http://localhost:8080/dashboard
```

#### Step 7: Server1 상태 확인

```bash
# 컨테이너 상태 확인
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 헬스 체크
curl http://localhost:8080/health
```

**예상 출력:**
```
{"status":"healthy","timestamp":"2025-11-17T15:30:00.123Z"}
```

### 전체 시스템 테스트

#### Step 1: 외부에서 대시보드 접속

로컬 브라우저에서 Server1의 대시보드에 접속합니다.

```bash
# 브라우저에서 열기
http://<SERVER1_PUBLIC_IP>:8080/dashboard
```

#### Step 2: 주문 생성 테스트 (로컬에서)

```bash
# 단일 주문 생성
curl -X POST http://<SERVER1_PUBLIC_IP>:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000
  }'
```

#### Step 3: 통계 확인

```bash
# 통계 조회
curl http://<SERVER1_PUBLIC_IP>:8080/stats | jq .
```

#### Step 4: 대량 테스트 (Server1에서)

```bash
# Server1 터미널에서
cd ~/kafka-event-architecture-lab

# 테스트 스크립트에 실행 권한 부여
chmod +x test-orders.sh

# 100개 주문 생성
SERVER_URL=http://localhost:8080 ./test-orders.sh 100

# 대시보드에서 실시간 통계 확인
```

#### Step 5: Kafka 메시지 확인 (Server2에서)

```bash
# Server2 터미널에서
# orders 토픽 메시지 확인
docker exec kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 10

# order-stats 토픽 메시지 확인
docker exec kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --max-messages 10
```

### 모니터링 및 관리

#### 컨테이너 로그 확인

```bash
# Server1에서
docker logs server1-app --tail 100 -f

# Server2에서
docker logs server2-streams --tail 100 -f
docker logs kafka-broker --tail 100 -f
```

#### 컨테이너 재시작

```bash
# Server1 재시작
docker restart server1-app

# Server2 재시작
docker restart server2-streams

# Kafka 재시작
docker restart kafka-broker

# PostgreSQL 재시작
docker restart postgres-orders
```

#### 리소스 사용량 확인

```bash
# 컨테이너별 리소스 사용량
docker stats

# 디스크 사용량
docker system df

# 네트워크 확인
docker network inspect kafka-network  # Server2에서
docker network inspect app-network    # Server1에서
```

### 배포 문제 해결

#### Kafka 연결 실패

**증상:** Server1에서 Kafka 연결 오류 발생

```bash
# Server1에서 Server2로 연결 테스트
telnet <SERVER2_PRIVATE_IP> 9092
# 또는
nc -zv <SERVER2_PRIVATE_IP> 9092
```

**해결 방법:**
1. Server2 보안 그룹에서 Server1의 보안 그룹이 9092 포트 접근 허용되어 있는지 확인
2. Kafka ADVERTISED_LISTENERS 설정 확인
3. Kafka 컨테이너 재시작

#### PostgreSQL 연결 실패

```bash
# Server1에서 PostgreSQL 연결 테스트
docker exec server1-app ping postgres-orders

# PostgreSQL 로그 확인
docker logs postgres-orders --tail 50
```

#### 포트 이미 사용 중

```bash
# 포트 사용 중인 프로세스 확인
sudo lsof -i :8080  # Server1
sudo lsof -i :9092  # Server2

# 프로세스 종료
sudo kill -9 <PID>
```

### 시스템 종료 및 정리

#### Server1 정리

```bash
# 컨테이너 중지 및 삭제
docker stop server1-app postgres-orders
docker rm server1-app postgres-orders

# 볼륨 삭제 (데이터 영구 삭제)
docker volume rm postgres-data

# 네트워크 삭제
docker network rm app-network

# 이미지 삭제
docker rmi server1-app:latest
```

#### Server2 정리

```bash
# 컨테이너 중지 및 삭제
docker stop server2-streams kafka-broker zookeeper
docker rm server2-streams kafka-broker zookeeper

# 네트워크 삭제
docker network rm kafka-network

# 이미지 삭제
docker rmi server2-streams:latest
```

---

## API 명세

### Server1 REST API

#### 1. 헬스 체크

```http
GET /health
```

**Response 200:**
```json
{
  "status": "healthy",
  "timestamp": "2025-11-17T15:30:00.123Z"
}
```

#### 2. 주문 생성

```http
POST /orders
Content-Type: application/json
```

**Request Body:**
```json
{
  "user_id": "user-001",
  "store_id": "store-777",
  "region": "Seoul",
  "price": 25000
}
```

**Response 201:**
```json
{
  "success": true,
  "order": {
    "order_id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "user_id": "user-001",
    "store_id": "store-777",
    "region": "Seoul",
    "price": 25000,
    "status": "CREATED",
    "created_at": "2025-11-17T15:30:00.123Z"
  }
}
```

**Response 400 (Bad Request):**
```json
{
  "error": "Missing required fields: user_id, store_id, region, price"
}
```

#### 3. 주문 목록 조회

```http
GET /orders?limit=100
```

**Query Parameters:**
- `limit` (optional): 조회할 주문 수 (기본값: 100)

**Response 200:**
```json
{
  "success": true,
  "count": 10,
  "orders": [
    {
      "order_id": "...",
      "user_id": "user-001",
      "store_id": "store-777",
      "region": "Seoul",
      "price": 25000,
      "status": "CREATED",
      "created_at": "2025-11-17T15:30:00.123Z"
    }
  ]
}
```

#### 4. 통계 조회

```http
GET /stats
```

**Response 200:**
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
      },
      "Incheon": {
        "orders": 20,
        "sales": 500000
      }
    },
    "lastUpdated": "2025-11-17T15:30:00.123Z"
  }
}
```

#### 5. 대시보드 UI

```http
GET /dashboard
```

HTML 페이지 반환

---

## 트러블슈팅

### 문제 1: Kafka 연결 시간 초과

**증상:**
```
Error: Connection timeout for kafka:9092
```

**원인:**
- Kafka 브로커가 아직 시작 중
- 잘못된 Kafka 주소 설정
- 네트워크 문제

**해결:**
```bash
# 1. Kafka 상태 확인
docker logs kafka-broker --tail 50

# 2. Kafka가 완전히 시작될 때까지 대기 (약 30초)

# 3. Kafka 연결 테스트
docker exec kafka-broker kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# 4. Server1 재시작
docker restart server1-app
```

### 문제 2: PostgreSQL 초기화 실패

**증상:**
```
Error: relation "orders" does not exist
```

**원인:**
- 데이터베이스 스키마가 생성되지 않음
- PostgreSQL 연결 실패

**해결:**
```bash
# 1. PostgreSQL 상태 확인
docker logs postgres-orders --tail 50

# 2. PostgreSQL 접속하여 수동 테이블 생성
docker exec -it postgres-orders psql -U orders_user -d orders_db

# SQL 실행
CREATE TABLE IF NOT EXISTS orders (
  order_id VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  store_id VARCHAR(255) NOT NULL,
  region VARCHAR(100) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

\q  # 종료

# 3. Server1 재시작
docker restart server1-app
```

### 문제 3: Kafka Streams 재시작 후 중복 처리

**증상:**
통계가 갑자기 2배로 증가

**원인:**
Kafka Streams 애플리케이션이 토픽을 처음부터 다시 읽음

**해결:**
```bash
# Kafka Streams 상태 저장소 삭제
docker exec kafka-broker kafka-streams-application-reset \
  --application-id order-stats-app \
  --input-topics orders \
  --bootstrap-servers localhost:9092

# Server2 재시작
docker restart server2-streams
```

### 문제 4: Server2 (Kafka Streams) 재시작 반복

**증상:**
```
Server2 컨테이너가 계속 재시작됨
로그에 "UNKNOWN_TOPIC_OR_PARTITION" 에러 반복
```

**원인:**
- 단일 브로커 환경에서 replication factor 기본값(3)이 설정되어 있음
- Kafka Streams 내부 토픽(repartition, changelog) 생성 실패

**해결:**
이 문제는 이미 코드에 반영되어 있지만, 만약 발생한다면:

```java
// server2-streams/src/main/java/com/example/streams/OrderStatsApp.java에 추가
props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);
```

그리고 Server2 재빌드:
```bash
cd infra-local
docker-compose stop server2
docker-compose build server2
docker-compose up -d server2
```

### 문제 5: 포트 충돌

**증상:**
```
Error: Port 8080 is already in use
```

**해결:**
```bash
# 1. 포트 사용 중인 프로세스 확인
sudo lsof -i :8080

# 2. 프로세스 종료
sudo kill -9 <PID>

# 3. 또는 다른 포트 사용
docker run -d \
  --name server1-app \
  -p 8081:8080 \
  ...
```

### 문제 5: 컨테이너 메모리 부족

**증상:**
```
docker: Error response from daemon: OOM command not allowed when used memory > 'maxmemory'.
```

**해결:**
```bash
# 1. 메모리 사용량 확인
docker stats

# 2. 메모리 제한 설정
docker run -d \
  --name server2-streams \
  --memory="2g" \
  --memory-swap="2g" \
  ...

# 3. 불필요한 컨테이너 정리
docker system prune -a
```

### 문제 6: 대시보드에 통계가 표시되지 않음

**원인:**
- Kafka Consumer가 order-stats 토픽을 구독하지 못함
- Server2 Streams가 통계를 발행하지 않음

**해결:**
```bash
# 1. Server1 로그 확인
docker logs server1-app | grep "order-stats"

# 2. Server2 로그 확인
docker logs server2-streams | grep "Publishing stats"

# 3. order-stats 토픽에 메시지가 있는지 확인
docker exec kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-stats \
  --from-beginning \
  --max-messages 5

# 4. 주문을 새로 생성하여 이벤트 발생시키기
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test",
    "store_id": "test",
    "region": "Seoul",
    "price": 10000
  }'
```

---

## 추가 학습 자료

### Kafka Streams 개념

이 프로젝트에서 사용된 Kafka Streams 핵심 개념:

1. **KStream**: 무한한 레코드 스트림
2. **KTable**: 변경 가능한 상태 테이블
3. **Aggregation**: 데이터 집계 (sum, count 등)
4. **Grouping**: 키 기반 그룹핑
5. **Stateful Processing**: 상태 저장 처리

### 이벤트 기반 아키텍처 장점

1. **느슨한 결합**: 서비스 간 독립성
2. **확장성**: 수평적 확장 용이
3. **비동기 처리**: 응답 시간 개선
4. **장애 격리**: 한 서비스 장애가 전체에 영향 X
5. **이벤트 소싱**: 모든 변경 이력 추적 가능

### 실습 확장 아이디어

1. **지속성 향상**:
   - RocksDB State Store 사용
   - Kafka Connect로 DB 동기화

2. **모니터링 추가**:
   - Prometheus + Grafana
   - Kafka JMX Metrics
   - APM 도구 연동

3. **고가용성 구성**:
   - Kafka 클러스터 (3+ 브로커)
   - Replication Factor 설정
   - Multi-AZ 배포

4. **보안 강화**:
   - Kafka SSL/TLS 암호화
   - SASL 인증
   - API 인증/인가

---

## 라이센스

MIT License

---

## 문의

이슈 또는 질문이 있으시면 GitHub Issues를 통해 문의해 주세요.

**제작자:** Kafka Event Architecture Lab Team
**버전:** 1.0.0
**최종 업데이트:** 2025-11-17
