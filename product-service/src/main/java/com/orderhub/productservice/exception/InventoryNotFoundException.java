package com.orderhub.productservice.exception;

// Ürün var ama inventory-service'te o sku'ya ait kayıt yok → handler 404'e çevirir
public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String sku) {
        super("Inventory not found for sku: " + sku);
    }
}
