package com.orderhub.productservice.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) { // sadece factory kullanır, dışarı kapalı
        super("Product not found: " + id);
    }

    private ProductNotFoundException(String message) {   // sadece factory kullanır, dışarı kapalı
        super(message);
    }

    public static ProductNotFoundException forSku(String sku) {  // çağıran yerde ne aradığımız okunur
        return new ProductNotFoundException("Product not found with sku: " + sku);
    }
}