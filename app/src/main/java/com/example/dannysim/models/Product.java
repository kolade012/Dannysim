package com.example.dannysim.models;

public class Product {
    private String name;
    private int soldQuantity;

    public Product(String name, int soldQuantity) {
        this.name = name;
        this.soldQuantity = soldQuantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(int soldQuantity) {
        this.soldQuantity = soldQuantity;
    }
}