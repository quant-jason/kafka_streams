require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const path = require('path');

const { initializeDatabase, insertOrder, getRecentOrders } = require('./db');
const { connectProducer, publishOrderEvent, disconnectProducer } = require('./kafka-producer');
const { startStatsConsumer, getLatestStats, disconnectConsumer, setEventLog } = require('./kafka-consumer');

const app = express();
const PORT = process.env.PORT || 8080;

// In-memory event log for KStream/KTable visualization
const eventLog = {
  recentOrders: [],      // KStream: 최근 주문 이벤트
  recentStats: [],       // KTable: 상태 변경 이력
  maxLogSize: 50
};

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// Create order endpoint
app.post('/orders', async (req, res) => {
  try {
    const { user_id, store_id, region, price } = req.body;

    // Validation
    if (!user_id || !store_id || !region || !price) {
      return res.status(400).json({
        error: 'Missing required fields: user_id, store_id, region, price',
      });
    }

    // Create order object
    const order = {
      order_id: uuidv4(),
      user_id,
      store_id,
      region,
      price: parseFloat(price),
      status: 'CREATED',
      created_at: new Date().toISOString(),
    };

    // 1. Save to database
    await insertOrder(order);
    console.log(`💾 Order saved to DB: ${order.order_id}`);

    // 2. Publish to Kafka
    await publishOrderEvent(order);

    // 3. Add to event log (KStream visualization)
    eventLog.recentOrders.unshift({
      ...order,
      timestamp: new Date().toISOString(),
      step: 'PRODUCED'
    });
    if (eventLog.recentOrders.length > eventLog.maxLogSize) {
      eventLog.recentOrders.pop();
    }

    res.status(201).json({
      success: true,
      order,
    });
  } catch (error) {
    console.error('❌ Error creating order:', error);
    res.status(500).json({
      error: 'Failed to create order',
      message: error.message,
    });
  }
});

// Get recent orders
app.get('/orders', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;
    const orders = await getRecentOrders(limit);
    res.json({
      success: true,
      count: orders.length,
      orders,
    });
  } catch (error) {
    console.error('❌ Error fetching orders:', error);
    res.status(500).json({
      error: 'Failed to fetch orders',
      message: error.message,
    });
  }
});

// Get statistics from Kafka Streams
app.get('/stats', (req, res) => {
  const stats = getLatestStats();
  res.json({
    success: true,
    stats,
  });
});

// Get event log for KStream/KTable visualization
app.get('/events', (req, res) => {
  res.json({
    success: true,
    kstream: eventLog.recentOrders.slice(0, 20),
    ktable: eventLog.recentStats.slice(0, 20)
  });
});

// Dashboard page
app.get('/dashboard', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/dashboard.html'));
});

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('🛑 SIGTERM received, shutting down gracefully...');
  await disconnectProducer();
  await disconnectConsumer();
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('🛑 SIGINT received, shutting down gracefully...');
  await disconnectProducer();
  await disconnectConsumer();
  process.exit(0);
});

// Start server
async function startServer() {
  try {
    // Initialize database
    await initializeDatabase();

    // Connect Kafka producer
    await connectProducer();

    // Set event log reference for consumer
    setEventLog(eventLog);

    // Start Kafka consumer for stats
    await startStatsConsumer();

    // Start Express server
    app.listen(PORT, () => {
      console.log(`🚀 Server1 running on http://localhost:${PORT}`);
      console.log(`📊 Dashboard: http://localhost:${PORT}/dashboard`);
    });
  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
}

startServer();
