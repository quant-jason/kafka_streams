const { Kafka } = require('kafkajs');

const kafka = new Kafka({
  clientId: process.env.KAFKA_CLIENT_ID || 'server1-consumer',
  brokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || 'localhost:9092').split(','),
});

const consumer = kafka.consumer({ groupId: 'server1-stats-consumer' });

// In-memory cache for latest statistics
let latestStats = {
  totalOrders: 0,
  totalSales: 0,
  byRegion: {},
  lastUpdated: null,
};

// Event log for KTable visualization (shared with main app)
let eventLogRef = null;

function setEventLog(eventLog) {
  eventLogRef = eventLog;
}

async function startStatsConsumer() {
  try {
    await consumer.connect();
    await consumer.subscribe({ topic: 'order-stats', fromBeginning: false });

    console.log('✅ Kafka Consumer connected and subscribed to order-stats');

    await consumer.run({
      eachMessage: async ({ topic, partition, message }) => {
        try {
          const stats = JSON.parse(message.value.toString());

          // Update in-memory stats
          latestStats = {
            ...stats,
            lastUpdated: new Date().toISOString(),
          };

          console.log('📥 Received stats update:', stats);

          // Add to event log (KTable visualization)
          if (eventLogRef) {
            eventLogRef.recentStats.unshift({
              ...stats,
              timestamp: new Date().toISOString(),
              step: 'KTABLE_UPDATE'
            });
            if (eventLogRef.recentStats.length > eventLogRef.maxLogSize) {
              eventLogRef.recentStats.pop();
            }
          }
        } catch (error) {
          console.error('❌ Error processing stats message:', error);
        }
      },
    });
  } catch (error) {
    console.error('❌ Kafka Consumer error:', error);
    throw error;
  }
}

function getLatestStats() {
  return latestStats;
}

async function disconnectConsumer() {
  await consumer.disconnect();
  console.log('🔌 Kafka Consumer disconnected');
}

module.exports = {
  startStatsConsumer,
  getLatestStats,
  disconnectConsumer,
  setEventLog,
};
