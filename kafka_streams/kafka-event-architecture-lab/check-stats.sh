#!/bin/bash

# Script to check current statistics
# Usage: ./check-stats.sh

SERVER_URL="${SERVER_URL:-http://localhost:8080}"

echo "📊 Fetching current statistics from $SERVER_URL/stats..."
echo ""

RESPONSE=$(curl -s "$SERVER_URL/stats")

if [ $? -eq 0 ]; then
  echo "Response:"
  echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
else
  echo "❌ Failed to fetch statistics"
  exit 1
fi

echo ""
echo "📊 Dashboard URL: $SERVER_URL/dashboard"
