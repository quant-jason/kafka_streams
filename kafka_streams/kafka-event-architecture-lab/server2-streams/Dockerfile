FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src ./src

# Build the application
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM amazoncorretto:17

WORKDIR /app

# Install required libraries for RocksDB
RUN yum install -y glibc && yum clean all

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/order-stats-app.jar app.jar

# Environment variables
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV APPLICATION_ID=order-stats-app

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
