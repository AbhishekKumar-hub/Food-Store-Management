package com.foody.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String description;
    private String category;
    private double price;
    private String imageUrl;
    private boolean available;
    private int stock;
    private boolean vegetarian;
    private int preparationTimeMinutes = 25;

    @Transient
    private double averageRating;

    @Transient
    private int reviewCount;

    @Transient
    private Map<Integer, Long> ratingBreakdown = new LinkedHashMap<>();

    public Product() {}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public int getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public void setPreparationTimeMinutes(int preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Map<Integer, Long> getRatingBreakdown() {
        return ratingBreakdown;
    }

    public void setRatingBreakdown(Map<Integer, Long> ratingBreakdown) {
        this.ratingBreakdown = ratingBreakdown == null ? new LinkedHashMap<>() : ratingBreakdown;
    }
}
