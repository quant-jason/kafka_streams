plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kafka Streams
    implementation("org.apache.kafka:kafka-streams:3.6.1")
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("org.slf4j:slf4j-simple:2.0.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.6.1")
}

application {
    mainClass.set("com.example.streams.OrderStatsApp")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("order-stats-app")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
