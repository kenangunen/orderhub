package com.orderhub.productservice.client;

// inventory-service'in JSON cevabının, product-service'in ihtiyaç duyduğu kısmı.
// reservedQty ve updatedAt'i almıyoruz; Jackson tanımadığı alanları yok sayar.
record InventoryStockResponse(
        String sku, int availableQty
) {
}
