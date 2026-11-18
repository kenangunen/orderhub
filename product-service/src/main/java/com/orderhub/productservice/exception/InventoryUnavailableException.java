package com.orderhub.productservice.exception;

// inventory-service'e ulaşılamadı (bağlantı, timeout, 5xx) → handler 503'e çevirir
public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(String sku, Throwable cause) {
        super("Inventory service is unavailable for sku: " + sku, cause);   // cause: asıl hata kaybolmasın
    }
}