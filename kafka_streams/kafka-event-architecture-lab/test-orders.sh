#!/bin/bash

# Test script to create sample orders
# Usage: ./test-orders.sh [number_of_orders]

SERVER_URL="${SERVER_URL:-http://localhost:8080}"
NUM_ORDERS="${1:-10}"

REGIONS=("Seoul" "Busan" "Incheon" "Daegu" "Gwangju")
STORES=("store-001" "store-002" "store-003" "store-777" "store-999")
USERS=("user-001" "user-002" "user-003" "user-004" "user-005")

echo "📦 Creating $NUM_ORDERS sample orders..."
echo "🔗 Server URL: $SERVER_URL"
echo ""

for i in $(seq 1 $NUM_ORDERS); do
  # Random selection
  REGION=${REGIONS[$RANDOM % ${#REGIONS[@]}]}
  STORE=${STORES[$RANDOM % ${#STORES[@]}]}
  USER=${USERS[$RANDOM % ${#USERS[@]}]}
  PRICE=$((RANDOM % 50000 + 5000))

  # Create order
  RESPONSE=$(curl -s -X POST "$SERVER_URL/orders" \
    -H "Content-Type: application/json" \
    -d "{
      \"user_id\": \"$USER\",
      \"store_id\": \"$STORE\",
      \"region\": \"$REGION\",
      \"price\": $PRICE
    }")

  if echo "$RESPONSE" | grep -q "success"; then
    ORDER_ID=$(echo "$RESPONSE" | grep -o '"order_id":"[^"]*' | cut -d'"' -f4)
    echo "✅ Order $i created: $ORDER_ID | $REGION | ₩$PRICE"
  else
    echo "❌ Order $i failed: $RESPONSE"
  fi

  # Small delay to avoid overwhelming the system
  sleep 0.5
done

echo ""
echo "✅ Created $NUM_ORDERS orders"
echo "📊 Check dashboard at: $SERVER_URL/dashboard"
