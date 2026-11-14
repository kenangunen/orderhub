package com.orderhub.inventoryservice.inventory.service;

import com.orderhub.inventoryservice.inventory.InventoryItem;
import com.orderhub.inventoryservice.inventory.dto.InventoryResponse;
import com.orderhub.inventoryservice.exception.InventoryItemNotFoundException;
import com.orderhub.inventoryservice.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envanter (Inventory) ile ilgili tüm iş kurallarını (business logic) yöneten servis sınıfıdır.
 * <p>
 * Sorumlulukları:
 * - Ürün envanter sorgularını yürütmek.
 * - Veritabanı ve DTO katmanları arasındaki veri dönüşümlerini koordine etmek.
 * - Kayıt bulunamadığı durumlarda uygun hata (exception) mekanizmalarını tetiklemek.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryResponse getBySku(String sku) {
        log.info("Getting product with sku {}", sku);

        return InventoryResponse.from(findInventoryItemOrThrow(sku));
    }

    public List<InventoryResponse> getAll() {
        log.info("Getting all products");

        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    private InventoryItem findInventoryItemOrThrow(String sku) {
        return inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryItemNotFoundException(sku));
    }
}
