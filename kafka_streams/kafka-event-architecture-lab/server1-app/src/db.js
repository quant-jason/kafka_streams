const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.POSTGRES_HOST || 'localhost',
  port: process.env.POSTGRES_PORT || 5432,
  database: process.env.POSTGRES_DB || 'orders_db',
  user: process.env.POSTGRES_USER || 'orders_user',
  password: process.env.POSTGRES_PASSWORD || 'orders_pass',
});

// Initialize database schema
async function initializeDatabase() {
  const client = await pool.connect();
  try {
    await client.query(`
      CREATE TABLE IF NOT EXISTS orders (
        order_id VARCHAR(255) PRIMARY KEY,
        user_id VARCHAR(255) NOT NULL,
        store_id VARCHAR(255) NOT NULL,
        region VARCHAR(100) NOT NULL,
        price DECIMAL(10, 2) NOT NULL,
        status VARCHAR(50) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✅ Database schema initialized');
  } catch (error) {
    console.error('❌ Database initialization error:', error);
    throw error;
  } finally {
    client.release();
  }
}

// Insert order into database
async function insertOrder(order) {
  const client = await pool.connect();
  try {
    const query = `
      INSERT INTO orders (order_id, user_id, store_id, region, price, status, created_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const values = [
      order.order_id,
      order.user_id,
      order.store_id,
      order.region,
      order.price,
      order.status,
      order.created_at,
    ];
    const result = await client.query(query, values);
    return result.rows[0];
  } finally {
    client.release();
  }
}

// Get recent orders
async function getRecentOrders(limit = 100) {
  const client = await pool.connect();
  try {
    const result = await client.query(
      'SELECT * FROM orders ORDER BY created_at DESC LIMIT $1',
      [limit]
    );
    return result.rows;
  } finally {
    client.release();
  }
}

module.exports = {
  initializeDatabase,
  insertOrder,
  getRecentOrders,
  pool,
};
