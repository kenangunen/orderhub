package com.orderhub.inventoryservice.inventory.repository;

import com.orderhub.inventoryservice.inventory.InventoryItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, InventoryItem> inventoryItems = new ConcurrentHashMap<>();

    @Override
    public InventoryItem save(InventoryItem inventoryItem) {
        inventoryItems.put(inventoryItem.getSku(), inventoryItem);
        return inventoryItem;
    }

    @Override
    public Optional<InventoryItem> findBySku(String sku) {
        return Optional.ofNullable(inventoryItems.get(sku));
    }

    @Override
    public List<InventoryItem> findAll() {
        return List.copyOf(inventoryItems.values());
    }
}
