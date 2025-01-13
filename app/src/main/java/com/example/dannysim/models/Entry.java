package com.example.dannysim.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Entry implements Serializable {
    private String id;
    private String date;
    private String controlNumber;
    private String entryType;
    private String driver;
    private long createdAt;
    private int month;
    private Map<String, Map<String, Object>> products;

    // Required empty constructor for Firebase
    public Entry(String entryId, String entryDate, String productName, int controlNum, int soldQuantity, String entryType) {}

    // Constructor for creating entries from Firebase data
    public Entry(String id, String date, String controlNumber, String entryType,
                 String driver, long createdAt, int month) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.month = month;
        this.products = new HashMap<>();
    }

    // Constructor for list display
    public Entry(String id, String date, String controlNumber, String entryType,
                 String driver, int createdAt) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.products = new HashMap<>();
    }

    // Add a product to the entry
    public void addProduct(String index, String productName, int inQuantity, int outQuantity, int soldQuantity) {
        Map<String, Object> productMap = new HashMap<>();
        productMap.put("product", productName);
        productMap.put("in", inQuantity);
        productMap.put("out", outQuantity);
        productMap.put("sold", soldQuantity);
        this.products.put(index, productMap);
    }

    // Get a specific product's details
    public Map<String, Object> getProduct(String index) {
        return products.get(index);
    }

    // Get the first product name (for list display)
    public String getFirstProductName() {
        if (products != null && !products.isEmpty()) {
            Map<String, Object> firstProduct = products.get("0");
            if (firstProduct != null) {
                return (String) firstProduct.get("product");
            }
        }
        return "N/A";
    }

    // Get the first product's sold quantity (for list display)
    public int getFirstProductSoldQuantity() {
        if (products != null && !products.isEmpty()) {
            Map<String, Object> firstProduct = products.get("0");
            if (firstProduct != null) {
                Object soldObj = firstProduct.get("sold");
                if (soldObj instanceof Long) {
                    return ((Long) soldObj).intValue();
                } else if (soldObj instanceof Integer) {
                    return (Integer) soldObj;
                }
            }
        }
        return 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(String controlNumber) {
        this.controlNumber = controlNumber;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public Map<String, Map<String, Object>> getProducts() {
        return products;
    }

    public void setProducts(Map<String, Map<String, Object>> products) {
        this.products = products;
    }
}