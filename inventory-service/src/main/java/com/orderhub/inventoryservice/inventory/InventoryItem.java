package com.orderhub.inventoryservice.inventory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class InventoryItem {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final String sku;

    private int availableQty;
    private int reservedQty;
    private Instant updatedAt;

    private InventoryItem(String sku, int availableQty, int reservedQty, Instant updatedAt) {
        this.sku = requireText(sku, "sku");
        this.availableQty = requireNonNegative(availableQty, "availableQty");
        this.reservedQty = requireNonNegative(reservedQty, "reservedQty");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static InventoryItem create(String sku, int availableQty) {
        Instant now = Instant.now();

        return new InventoryItem(sku, availableQty, 0, now);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }
}
