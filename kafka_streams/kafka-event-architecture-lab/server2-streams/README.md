# Server2 - Kafka Streams Processor

Kafka Streams를 사용한 실시간 주문 통계 처리 서버입니다.

## 주요 기능

### 실시간 스트림 처리

- **Input**: orders 토픽에서 주문 이벤트 수신
- **Processing**:
  - 총 주문 수 집계
  - 총 매출 계산
  - 지역별 통계 분석
- **Output**: order-stats 토픽으로 통계 결과 발행

## 동작 원리

```
orders 토픽
   ↓
[Kafka Streams 처리]
   ↓
1. 주문 데이터 읽기 (KStream)
   ↓
2. 그룹화 (groupBy)
   ↓
3. 집계 (aggregate)
   - totalOrders += 1
   - totalSales += price
   - byRegion[region].orders += 1
   - byRegion[region].sales += price
   ↓
4. 결과 발행 (KTable → KStream)
   ↓
order-stats 토픽
```

## Kafka Streams 토폴로지

### 처리 흐름

```java
KStream<String, Order> ordersStream
  ↓
filter (null 체크)
  ↓
groupBy ("global" key)
  ↓
aggregate (통계 계산)
  ↓
KTable<String, OrderStats>
  ↓
toStream()
  ↓
to (order-stats 토픽)
```

### Stateful Processing

- **State Store**: RocksDB 기반 로컬 상태 저장소
- **변경 로그**: Kafka 토픽에 상태 변경 기록
- **복구**: 재시작 시 상태 자동 복구

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 17 이상
- Gradle 8.5 이상
- Kafka 브로커 (실행 중)

### 환경 변수 설정

```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export APPLICATION_ID=order-stats-app
```

### 빌드 및 실행

```bash
# Gradle로 빌드
./gradlew clean shadowJar

# JAR 실행
java -jar build/libs/order-stats-app.jar
```

## Docker로 실행

### 이미지 빌드

```bash
docker build -t server2-streams:latest .
```

### 컨테이너 실행

```bash
docker run -d \
  --name server2-streams \
  -e KAFKA_BOOTSTRAP_SERVERS=<kafka_broker>:9092 \
  -e APPLICATION_ID=order-stats-app \
  server2-streams:latest
```

## 로그 확인

### 주문 수신 로그

```
📥 Received order: uuid-123 | Region: Seoul | Price: 25000.0
```

### 통계 업데이트 로그

```
📊 Updated stats - Total Orders: 10 | Total Sales: 250000.0
```

### 통계 발행 로그

```
📤 Publishing stats: OrderStats{totalOrders=10, totalSales=250000.0, ...}
```

## 데이터 모델

### Order (Input)

```java
{
  "order_id": "uuid-123",
  "user_id": "user-001",
  "store_id": "store-777",
  "region": "Seoul",
  "price": 25000.0,
  "status": "CREATED",
  "created_at": "2025-11-17T14:30:00Z"
}
```

### OrderStats (Output)

```java
{
  "totalOrders": 100,
  "totalSales": 2500000.0,
  "byRegion": {
    "Seoul": {
      "orders": 50,
      "sales": 1250000.0
    },
    "Busan": {
      "orders": 30,
      "sales": 750000.0
    }
  }
}
```

## 디렉토리 구조

```
server2-streams/
├── src/main/
│   ├── java/com/example/streams/
│   │   ├── OrderStatsApp.java         # 메인 애플리케이션
│   │   ├── model/
│   │   │   ├── Order.java             # 주문 모델
│   │   │   └── OrderStats.java        # 통계 모델
│   │   └── serde/
│   │       ├── JsonSerializer.java    # JSON 직렬화
│   │       ├── JsonDeserializer.java  # JSON 역직렬화
│   │       └── JsonSerde.java         # Serde 래퍼
│   └── resources/
│       └── simplelogger.properties    # 로깅 설정
├── build.gradle.kts                   # Gradle 빌드 스크립트
├── settings.gradle.kts                # Gradle 설정
├── Dockerfile                         # Docker 이미지 빌드
└── README.md                          # 이 파일
```

## Kafka Streams 개념

### KStream vs KTable

- **KStream**: 레코드의 무한 스트림 (INSERT 전용)
- **KTable**: 변경 가능한 상태 테이블 (INSERT/UPDATE/DELETE)

이 프로젝트에서는:
- **KStream**: orders 토픽에서 주문 읽기
- **KTable**: 집계 결과 저장 (aggregate 연산 결과)

### Stateful vs Stateless

- **Stateless**: filter, map, flatMap 등
- **Stateful**: aggregate, count, join 등 (상태 저장소 필요)

이 프로젝트에서는 **Stateful** 처리를 사용합니다 (aggregate).

## 트러블슈팅

### Kafka 연결 실패

**문제:** `Connection timeout`

**해결:**
```bash
# Kafka 브로커 확인
docker logs kafka-broker --tail 50

# 연결 테스트
telnet <kafka_host> 9092
```

### RocksDB 초기화 실패

**문제:** `Failed to load RocksDB JNI library`

**해결:** Alpine 이미지 대신 일반 이미지 사용 (Dockerfile 수정됨)

### 토픽이 없음

**문제:** `MissingSourceTopicException`

**해결:**
```bash
# 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders \
  --partitions 3 \
  --replication-factor 1

docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic order-stats \
  --partitions 3 \
  --replication-factor 1
```

### 상태 저장소 리셋

**문제:** 통계가 누적되어 잘못된 값이 표시됨

**해결:**
```bash
# Streams 애플리케이션 중지
docker stop server2-streams

# 상태 리셋
docker exec kafka-broker kafka-streams-application-reset \
  --application-id order-stats-app \
  --input-topics orders \
  --bootstrap-servers localhost:9092

# 재시작
docker start server2-streams
```

## 성능 최적화

### 파티션 수 조정

```bash
# 처리량 증가를 위해 파티션 수 증가
kafka-topics --bootstrap-server localhost:9092 \
  --alter \
  --topic orders \
  --partitions 6
```

### 병렬 처리

```java
// StreamsConfig에서 설정
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);
```

### 커밋 간격 조정

```java
// 더 빠른 통계 갱신을 위해 커밋 간격 단축
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
```

## 의존성

- **Kafka Streams**: 3.6.1
- **Kafka Clients**: 3.6.1
- **Jackson**: JSON 처리
- **SLF4J**: 로깅
- **Gradle Shadow**: Fat JAR 빌드

## 라이센스

MIT
