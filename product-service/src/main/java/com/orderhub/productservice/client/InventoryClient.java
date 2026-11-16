package com.orderhub.productservice.client;

import java.util.Optional;

// Service'in gördüğü tek şey bu sözleşme: "sku ver, stok adedini al".
// Arkasında RestClient mi, Feign mi, mock mu olduğu service'i ilgilendirmez.
public interface InventoryClient {
    Optional<Integer> getAvailableQty(String sku);
}

