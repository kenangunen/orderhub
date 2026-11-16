package com.orderhub.productservice.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orderhub.productservice.product.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        // inventory'den gelir; null = bilinmiyor (kayıt yok ya da inventory'ye ulaşılamadı)
        @JsonInclude(JsonInclude.Include.NON_NULL)   // Sadece bu alan: değer null ise JSON'a hiç yazılmaz
        Integer quantityOnHand
) {
    // Stok bilgisi olmadan: create, update, liste gibi yerler bunu kullanmaya devam eder
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, Integer quantityOnHand) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                quantityOnHand
        );
    }
}