package com.agri.supplytracker.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ClassifierService {

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
        Map.entry("banana", "Fruits"),
        Map.entry("apple", "Fruits"),
        Map.entry("orange", "Fruits"),
        Map.entry("lemon", "Fruits"),
        Map.entry("mango", "Fruits"),
        Map.entry("grape", "Fruits"),
        Map.entry("pear", "Fruits"),
        Map.entry("peach", "Fruits"),
        Map.entry("strawberry", "Fruits"),
        Map.entry("watermelon", "Fruits"),
        Map.entry("broccoli", "Vegetables"),
        Map.entry("carrot", "Vegetables"),
        Map.entry("pottedplant", "Plants"),
        Map.entry("bottle", "Beverages"),
        Map.entry("wine glass", "Beverages"),
        Map.entry("cup", "Containers"),
        Map.entry("bowl", "Containers"),
        Map.entry("box", "Packaging"),
        Map.entry("backpack", "Bags"),
        Map.entry("suitcase", "Bags"),
        Map.entry("truck", "Transport"),
        Map.entry("car", "Transport"),
        Map.entry("train", "Transport"),
        Map.entry("boat", "Transport")
    );

    public String classifyProduct(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "Unknown";
        }
        for (String label : labels) {
            String category = CATEGORY_MAP.get(label.toLowerCase());
            if (category != null) {
                return category;
            }
        }
        // Return the first label capitalised as a best-effort category
        String first = labels.get(0);
        return Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase();
    }
}
