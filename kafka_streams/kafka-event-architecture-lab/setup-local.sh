#!/bin/bash

# Kafka Event Architecture Lab - 로컬 환경 자동 설정 스크립트
# 이 스크립트는 로컬 환경에서 전체 시스템을 자동으로 설정합니다.

set -e

echo "🚀 Kafka Event Architecture Lab - 로컬 환경 설정"
echo "=================================================="
echo ""

# 1. Docker 확인
echo "1️⃣  Docker 설치 확인..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되지 않았습니다."
    echo "   https://www.docker.com/products/docker-desktop 에서 Docker Desktop을 다운로드하세요."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker가 실행 중이 아닙니다."
    echo "   Docker Desktop을 실행하세요."
    exit 1
fi

echo "   ✅ Docker 확인 완료"
echo ""

# 2. Docker Compose 확인
echo "2️⃣  Docker Compose 확인..."
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose가 설치되지 않았습니다."
    exit 1
fi
echo "   ✅ Docker Compose 확인 완료"
echo ""

# 3. 기존 컨테이너 정리 (선택)
echo "3️⃣  기존 컨테이너 확인..."
if docker ps -a | grep -q "kafka-broker\|server1-app\|server2-streams"; then
    read -p "   기존 컨테이너가 발견되었습니다. 정리하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "   🧹 기존 컨테이너 정리 중..."
        cd infra-local
        docker-compose down -v 2>/dev/null || true
        cd ..
        echo "   ✅ 정리 완료"
    fi
else
    echo "   ✅ 정리 작업 불필요"
fi
echo ""

# 4. 시스템 시작
echo "4️⃣  전체 시스템 시작 중..."
echo "   ⏱️  초기 빌드는 3-5분 정도 소요됩니다..."
cd infra-local
docker-compose up -d --build

# 서비스 시작 대기
echo ""
echo "   ⏳ 서비스 시작 대기 중..."
sleep 30

cd ..
echo "   ✅ 시스템 시작 완료"
echo ""

# 5. 헬스 체크
echo "5️⃣  서비스 헬스 체크..."
sleep 5

# Kafka 헬스 체크
if docker exec kafka-broker kafka-broker-api-versions --bootstrap-server localhost:9092 &> /dev/null; then
    echo "   ✅ Kafka Broker - Healthy"
else
    echo "   ⚠️  Kafka Broker - 시작 중..."
    sleep 10
fi

# PostgreSQL 헬스 체크
if docker exec postgres-orders pg_isready -U orders_user -d orders_db &> /dev/null; then
    echo "   ✅ PostgreSQL - Healthy"
else
    echo "   ❌ PostgreSQL - 문제 발생"
fi

# Server1 헬스 체크
if curl -s http://localhost:8080/health &> /dev/null; then
    echo "   ✅ Server1 - Healthy"
else
    echo "   ⚠️  Server1 - 시작 중..."
fi

echo ""

# 6. Kafka 토픽 생성
echo "6️⃣  Kafka 토픽 생성..."

# orders 토픽
if docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "^orders$"; then
    echo "   ✅ orders 토픽 이미 존재"
else
    docker exec kafka-broker kafka-topics \
        --bootstrap-server localhost:9092 \
        --create --topic orders \
        --partitions 3 --replication-factor 1 \
        --if-not-exists &> /dev/null
    echo "   ✅ orders 토픽 생성 완료"
fi

# order-stats 토픽
if docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "^order-stats$"; then
    echo "   ✅ order-stats 토픽 이미 존재"
else
    docker exec kafka-broker kafka-topics \
        --bootstrap-server localhost:9092 \
        --create --topic order-stats \
        --partitions 3 --replication-factor 1 \
        --if-not-exists &> /dev/null
    echo "   ✅ order-stats 토픽 생성 완료"
fi

echo ""

# 7. Server2 재시작
echo "7️⃣  Server2 재시작 (토픽 인식)..."
docker restart server2-streams &> /dev/null
sleep 10
echo "   ✅ Server2 재시작 완료"
echo ""

# 8. 최종 상태 확인
echo "8️⃣  최종 상태 확인..."
./check-health.sh

echo ""
echo "=================================================="
echo "✅ 로컬 환경 설정 완료!"
echo "=================================================="
echo ""
echo "📊 대시보드: http://localhost:8080/dashboard"
echo ""
echo "🧪 테스트 실행:"
echo "   ./test-orders.sh 10"
echo ""
echo "📈 통계 확인:"
echo "   ./check-stats.sh"
echo ""
echo "🔍 로그 확인:"
echo "   docker logs server1-app -f"
echo "   docker logs server2-streams -f"
echo ""
echo "🛑 시스템 종료:"
echo "   cd infra-local && docker-compose down"
echo ""
