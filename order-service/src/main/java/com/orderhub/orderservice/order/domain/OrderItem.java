package com.orderhub.orderservice.order.domain;

import java.math.BigDecimal;

public record OrderItem(String sku, String productName, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku cannot be null or blank");
        }

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName cannot be null or blank");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        // signum(): negatifse -1, sıfırsa 0, pozitifse 1
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than 0");
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
