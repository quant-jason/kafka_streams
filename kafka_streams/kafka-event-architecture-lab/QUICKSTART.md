# Quick Start Guide

3분 안에 Kafka 이벤트 아키텍처 시스템을 실행하는 가이드입니다.

## 🚀 가장 빠른 방법 (자동 설정) ⭐

```bash
# 1. 프로젝트 클론
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab

# 2. 자동 설정 실행
./setup-local.sh
```

완료! 🎉 이제 http://localhost:8080/dashboard 로 접속하세요.

---

## 📝 수동 설정 (단계별)

### 1단계: 프로젝트 클론

```bash
git clone https://github.com/<your-org>/kafka-event-architecture-lab.git
cd kafka-event-architecture-lab
```

### 2단계: 시스템 실행

```bash
cd infra-local
docker-compose up -d --build
```

⏱️ **소요 시간**: 초기 빌드 3-5분, 이후 30초

### 3단계: 토픽 생성

```bash
# orders 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic orders \
  --partitions 3 --replication-factor 1

# order-stats 토픽 생성
docker exec kafka-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-stats \
  --partitions 3 --replication-factor 1

# Server2 재시작
docker restart server2-streams
```

## 4단계: 헬스 체크

```bash
cd ..
./check-health.sh
```

✅ 모든 서비스가 **Healthy** 상태여야 합니다.

## 5단계: 주문 생성

**방법 1: 웹 대시보드 사용** ⭐ (추천!)

브라우저에서 `http://localhost:8080/dashboard` 접속 후:
- **🎲 랜덤 주문 생성** 버튼을 클릭하여 즉시 주문 생성
- 또는 폼에 값을 입력하여 수동 주문 생성

실시간으로 다음을 확인:
- 처리 흐름도 애니메이션
- KStream 이벤트 스트림
- KTable 상태 업데이트
- 통계 즉시 반영

**방법 2: 쉘 스크립트 사용**

```bash
./test-orders.sh 10
```

📦 10개의 샘플 주문이 생성됩니다.

## 6단계: 통계 확인

```bash
./check-stats.sh
```

또는 브라우저에서 대시보드 접속:
```
http://localhost:8080/dashboard
```

## 🎉 완료!

이제 다음을 확인할 수 있습니다:
- ✅ Kafka Streams 처리 흐름 시각화 (Producer → KStream → GroupBy → Aggregate → KTable → Output)
- ✅ 웹 UI로 랜덤 주문 생성 (버튼 클릭 한 번!)
- ✅ KStream 실시간 이벤트 스트림 모니터링
- ✅ KTable 집계 상태 변경 이력
- ✅ 지역별 통계 자동 갱신 (3초마다)

## 자주 사용하는 명령어

### 로그 확인
```bash
# Server1 로그
docker logs server1-app -f

# Server2 로그
docker logs server2-streams -f

# Kafka 로그
docker logs kafka-broker -f
```

### 시스템 종료
```bash
cd infra-local
docker-compose down

# 데이터까지 삭제
docker-compose down -v
```

### 시스템 재시작
```bash
docker-compose restart
```

### 특정 서비스만 재시작
```bash
docker restart server1-app
docker restart server2-streams
```

## 문제 해결

### Server2가 재시작 중인 경우

```bash
# 로그 확인
docker logs server2-streams --tail 50

# 토픽 생성 여부 확인
docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list

# 토픽이 없으면 3단계로 돌아가서 생성
```

### 통계가 0으로 표시되는 경우

```bash
# Server2 로그 확인
docker logs server2-streams | grep "Publishing stats"

# Server2 재시작
docker restart server2-streams

# 새 주문 생성
./test-orders.sh 5
```

### 포트 충돌

다른 애플리케이션이 포트를 사용 중인 경우:
```bash
# 포트 사용 확인
lsof -i :8080  # Server1
lsof -i :9092  # Kafka
lsof -i :5432  # PostgreSQL
```

## 다음 단계

1. **API 테스트**: [README.md](README.md#api-명세) 참고
2. **코드 이해**: `server1-app/README.md`, `server2-streams/README.md` 참고
3. **AWS 배포**: [README.md](README.md#aws-배포-가이드) 참고

## 도움이 필요하신가요?

- 📖 전체 문서: [README.md](README.md)
- 🐛 문제 보고: GitHub Issues
- 💬 질문: Discussions
