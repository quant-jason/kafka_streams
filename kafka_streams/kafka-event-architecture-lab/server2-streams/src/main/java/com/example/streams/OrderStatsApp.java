package com.example.streams;

import com.example.streams.model.Order;
import com.example.streams.model.OrderStats;
import com.example.streams.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OrderStatsApp {
    private static final Logger logger = LoggerFactory.getLogger(OrderStatsApp.class);

    private static final String INPUT_TOPIC = "orders";
    private static final String OUTPUT_TOPIC = "order-stats";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static void main(String[] args) {
        // Configuration
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG,
                  getEnv("APPLICATION_ID", "order-stats-app"));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                  getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                  Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                  Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

        // Single broker configuration
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);

        // Create topology
        StreamsBuilder builder = new StreamsBuilder();
        buildTopology(builder);

        // Create and start streams
        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Kafka Streams application...");
            streams.close();
            logger.info("Application shutdown complete");
        }));

        // Start the application
        logger.info("Starting Kafka Streams application...");
        logger.info("Input topic: {}", INPUT_TOPIC);
        logger.info("Output topic: {}", OUTPUT_TOPIC);
        streams.start();

        logger.info("✅ Kafka Streams application started successfully");
    }

    private static void buildTopology(StreamsBuilder builder) {
        // Serdes
        JsonSerde<Order> orderSerde = new JsonSerde<>(Order.class);
        JsonSerde<OrderStats> statsSerde = new JsonSerde<>(OrderStats.class);

        // Read from orders topic
        KStream<String, Order> ordersStream = builder.stream(
            INPUT_TOPIC,
            Consumed.with(Serdes.String(), orderSerde)
        );

        // Log incoming orders
        ordersStream.foreach((key, order) -> {
            if (order != null) {
                logger.info("📥 Received order: {} | Region: {} | Price: {}",
                           order.getOrderId(), order.getRegion(), order.getPrice());
            }
        });

        // Aggregate statistics without windowing for simplicity
        // This will maintain a running total
        KTable<String, OrderStats> statsTable = ordersStream
            .filter((key, order) -> order != null && order.getRegion() != null)
            .groupBy(
                (key, order) -> "global", // Single key for global aggregation
                Grouped.with(Serdes.String(), orderSerde)
            )
            .aggregate(
                OrderStats::new,
                (key, order, stats) -> {
                    // Update total orders and sales
                    stats.setTotalOrders(stats.getTotalOrders() + 1);
                    stats.setTotalSales(stats.getTotalSales() + order.getPrice());

                    // Update region-specific stats
                    Map<String, OrderStats.RegionStats> byRegion = stats.getByRegion();
                    OrderStats.RegionStats regionStats = byRegion.getOrDefault(
                        order.getRegion(),
                        new OrderStats.RegionStats(0, 0.0)
                    );

                    regionStats.setOrders(regionStats.getOrders() + 1);
                    regionStats.setSales(regionStats.getSales() + order.getPrice());
                    byRegion.put(order.getRegion(), regionStats);

                    logger.info("📊 Updated stats - Total Orders: {} | Total Sales: {}",
                               stats.getTotalOrders(), stats.getTotalSales());

                    return stats;
                },
                Materialized.with(Serdes.String(), statsSerde)
            );

        // Convert KTable to KStream and send to output topic
        statsTable.toStream()
            .foreach((key, stats) -> {
                logger.info("📤 Publishing stats: {}", stats);
            });

        statsTable.toStream()
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), statsSerde));

        logger.info("✅ Topology built successfully");
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
