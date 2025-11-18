package com.example.streams.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDeserializer<T> implements Deserializer<T> {
    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializer.class);
    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonDeserializer(Class<T> type) {
        this.type = type;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readValue(data, type);
        } catch (Exception e) {
            logger.error("Error deserializing JSON to object: " + new String(data), e);
            return null;
        }
    }
}
