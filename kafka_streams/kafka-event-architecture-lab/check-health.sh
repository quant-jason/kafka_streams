#!/bin/bash

# Health check script for all services
# Usage: ./check-health.sh

echo "🏥 Checking service health..."
echo ""

# Check Server1
echo "1️⃣  Server1 (Order API):"
if curl -s http://localhost:8080/health | grep -q "healthy"; then
  echo "   ✅ Healthy"
else
  echo "   ❌ Unhealthy or not responding"
fi
echo ""

# Check PostgreSQL
echo "2️⃣  PostgreSQL:"
if docker exec postgres-orders pg_isready -U orders_user -d orders_db &>/dev/null; then
  echo "   ✅ Healthy"
else
  echo "   ❌ Unhealthy or not running"
fi
echo ""

# Check Kafka
echo "3️⃣  Kafka Broker:"
if docker exec kafka-broker kafka-broker-api-versions --bootstrap-server localhost:9092 &>/dev/null; then
  echo "   ✅ Healthy"
else
  echo "   ❌ Unhealthy or not running"
fi
echo ""

# Check Zookeeper
echo "4️⃣  Zookeeper:"
if docker exec zookeeper nc -z localhost 2181 &>/dev/null; then
  echo "   ✅ Healthy"
else
  echo "   ❌ Unhealthy or not running"
fi
echo ""

# Check Server2 (Kafka Streams)
echo "5️⃣  Server2 (Kafka Streams):"
if docker ps | grep -q server2-streams.*Up; then
  echo "   ✅ Running"
else
  echo "   ❌ Not running"
fi
echo ""

echo "📋 Summary:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAME|kafka|zookeeper|postgres|server"
