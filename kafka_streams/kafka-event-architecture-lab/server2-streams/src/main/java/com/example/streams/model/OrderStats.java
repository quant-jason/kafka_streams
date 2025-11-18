package com.example.streams.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class OrderStats {
    @JsonProperty("totalOrders")
    private long totalOrders;

    @JsonProperty("totalSales")
    private double totalSales;

    @JsonProperty("byRegion")
    private Map<String, RegionStats> byRegion;

    @JsonProperty("windowStart")
    private String windowStart;

    @JsonProperty("windowEnd")
    private String windowEnd;

    public OrderStats() {
        this.byRegion = new HashMap<>();
    }

    public OrderStats(long totalOrders, double totalSales, Map<String, RegionStats> byRegion) {
        this.totalOrders = totalOrders;
        this.totalSales = totalSales;
        this.byRegion = byRegion != null ? byRegion : new HashMap<>();
    }

    // Getters and Setters
    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public Map<String, RegionStats> getByRegion() {
        return byRegion;
    }

    public void setByRegion(Map<String, RegionStats> byRegion) {
        this.byRegion = byRegion;
    }

    public String getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(String windowStart) {
        this.windowStart = windowStart;
    }

    public String getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(String windowEnd) {
        this.windowEnd = windowEnd;
    }

    @Override
    public String toString() {
        return "OrderStats{" +
                "totalOrders=" + totalOrders +
                ", totalSales=" + totalSales +
                ", byRegion=" + byRegion +
                ", windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                '}';
    }

    public static class RegionStats {
        private long orders;
        private double sales;

        public RegionStats() {}

        public RegionStats(long orders, double sales) {
            this.orders = orders;
            this.sales = sales;
        }

        public long getOrders() {
            return orders;
        }

        public void setOrders(long orders) {
            this.orders = orders;
        }

        public double getSales() {
            return sales;
        }

        public void setSales(double sales) {
            this.sales = sales;
        }

        @Override
        public String toString() {
            return "RegionStats{" +
                    "orders=" + orders +
                    ", sales=" + sales +
                    '}';
        }
    }
}
