package com.orderhub.inventoryservice.inventory.repository;

import com.orderhub.inventoryservice.inventory.InventoryItem;

import java.util.List;
import java.util.Optional;

// Bu envanter verileriyle hangi veritabanı işlemlerini yapabileceğimizi söyleyen bir sözleşmedir; ancak arkada bu işin nasıl yapılacağını (MySQL, PostgreSQL vb.) burada söylemez, sadece ne yapılacağını belirtir.
public interface InventoryRepository {
    InventoryItem save(InventoryItem inventoryItem);

    Optional<InventoryItem> findBySku(String sku);

    List<InventoryItem> findAll();
}
