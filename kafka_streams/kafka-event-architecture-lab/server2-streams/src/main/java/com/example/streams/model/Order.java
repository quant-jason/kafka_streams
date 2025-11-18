package com.example.streams.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("store_id")
    private String storeId;

    private String region;

    private Double price;

    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    public Order() {}

    public Order(String orderId, String userId, String storeId, String region,
                 Double price, String status, String createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.storeId = storeId;
        this.region = region;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", storeId='" + storeId + '\'' +
                ", region='" + region + '\'' +
                ", price=" + price +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
