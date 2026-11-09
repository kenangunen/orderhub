package com.orderhub.productservice.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Product {

    private final UUID id;
    private final String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Product(UUID id, String sku, String name, String description,
                    BigDecimal price, String category, boolean active,
                    Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sku = requireText(sku, "sku");
        this.name = requireText(name, "name");
        this.description = description;
        this.price = requirePositive(price, "price");
        this.category = requireText(category, "category");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Product create(String sku, String name, String description,
                                 BigDecimal price, String category) {
        Instant now = Instant.now();
        return new Product(UUID.randomUUID(), sku, name, description,
                price, category, true, now, now);
    }

    public void updateDetails(String name, String description, BigDecimal price, String category) {
        String validName = requireText(name, "name");
        BigDecimal validPrice = requirePositive(price, "price");
        String validCategory = requireText(category, "category");

        this.name = validName;
        this.description = description;
        this.price = validPrice;
        this.category = validCategory;
        touch();
    }

    public void changePrice(BigDecimal newPrice) {
        this.price = requirePositive(newPrice, "price");
        touch();
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
        touch();
    }

    public void deactivate() {
        if (!this.active) {
            return;
        }
        this.active = false;
        touch();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }
}