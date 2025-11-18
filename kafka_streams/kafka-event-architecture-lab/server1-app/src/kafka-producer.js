const { Kafka } = require('kafkajs');

const kafka = new Kafka({
  clientId: process.env.KAFKA_CLIENT_ID || 'server1-producer',
  brokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || 'localhost:9092').split(','),
});

const producer = kafka.producer();

let isConnected = false;

async function connectProducer() {
  if (isConnected) return;

  try {
    await producer.connect();
    isConnected = true;
    console.log('✅ Kafka Producer connected');
  } catch (error) {
    console.error('❌ Kafka Producer connection error:', error);
    throw error;
  }
}

async function publishOrderEvent(order) {
  if (!isConnected) {
    await connectProducer();
  }

  try {
    await producer.send({
      topic: 'orders',
      messages: [
        {
          key: order.order_id,
          value: JSON.stringify(order),
          timestamp: new Date(order.created_at).getTime().toString(),
        },
      ],
    });
    console.log(`📤 Published order event: ${order.order_id}`);
  } catch (error) {
    console.error('❌ Error publishing order event:', error);
    throw error;
  }
}

async function disconnectProducer() {
  if (isConnected) {
    await producer.disconnect();
    isConnected = false;
    console.log('🔌 Kafka Producer disconnected');
  }
}

module.exports = {
  connectProducer,
  publishOrderEvent,
  disconnectProducer,
};
