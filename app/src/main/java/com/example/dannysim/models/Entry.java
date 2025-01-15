package com.example.dannysim.models;

import java.io.Serializable;
import java.util.List;

public class Entry implements Serializable {

    private String id;
    private String date;
    private int controlNumber;
    private String entryType;
    private String driver;
    private long createdAt;
    private int month;
    private List<Product> products; // Use List<Product> for products

    // Required empty constructor for Firebase (if needed)
    public Entry(String entryId, String entryDate, int controlNum, String entryType, String driver, Long createdAtLong, List<Product> productList) {
    }

    // Constructor for creating entries from Firebase data
    public Entry(String id, String date, int controlNumber, String entryType,
                 String driver, long createdAt, int month) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.month = month;
        this.products = java.util.Collections.emptyList(); // Initialize empty list
    }

    // Constructor for list display (if needed)
    public Entry(String id, String date, int controlNumber, String entryType,
                 String driver, int createdAt) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.products = java.util.Collections.emptyList(); // Initialize empty list
    }

    // Add a product to the entry
    public void addProduct(Product product) {
        if (products != null) {
            products.add(product);
        }
    }

    // Get a specific product's details
    public Product getProduct(int index) {
        if (products != null && index >= 0 && index < products.size()) {
            return products.get(index);
        }
        return null;
    }

    // Get the first product name (for list display)
    public String getFirstProductName() {
        if (products != null && !products.isEmpty()) {
            return products.get(0).getName();
        }
        return "N/A";
    }

    // Get the first product's sold quantity (for list display)
    public int getFirstProductSoldQuantity() {
        if (products != null && !products.isEmpty()) {
            return products.get(0).getSoldQuantity();
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

    public int getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(int controlNumber) {
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

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}