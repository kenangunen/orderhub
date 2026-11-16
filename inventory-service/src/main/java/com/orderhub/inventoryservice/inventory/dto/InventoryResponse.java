package com.orderhub.inventoryservice.inventory.dto;

import com.orderhub.inventoryservice.inventory.InventoryItem;

import java.time.Instant;

/**
 * Envanter verilerini dış dünyaya (API istemcilerine) güvenli ve temiz bir
 * sözleşmeyle sunmak için kullanılan Response DTO (Data Transfer Object) sınıfıdır.
 * Amaç: İç veritabanı/domain modelini doğrudan dışarı sızdırmamak ve sadece
 * istemcinin görmesi gereken alanları (sku, stok miktarları, güncellenme tarihi) paylaşmaktır.
 */
public record InventoryResponse(
        String sku,
        int availableQty,
        int reservedQty,
        Instant updatedAt
) {
    public static InventoryResponse from(InventoryItem inventoryItem) {
        return new InventoryResponse(
                inventoryItem.getSku(),
                inventoryItem.getAvailableQty(),
                inventoryItem.getReservedQty(),
                inventoryItem.getUpdatedAt()
        );
    }
}
