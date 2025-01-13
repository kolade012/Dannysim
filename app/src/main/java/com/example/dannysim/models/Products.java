package com.example.dannysim.models;

import java.io.Serializable;
import java.util.Map;

public class Products implements Serializable {
    private Map<String, ProductItem> items;

    public Products() {
        // Required empty constructor for Firebase
    }

    public Map<String, ProductItem> getItems() { return items; }
    public void setItems(Map<String, ProductItem> items) { this.items = items; }
}
