package com.orderhub.productservice.product.dto;

import java.util.UUID;

// Sadece stok bilgisi; ürün detayı istenirse GET /products/{id}
public record ProductInventoryResponse(
        UUID productId,
        String sku,
        int availableQty
) {
}
