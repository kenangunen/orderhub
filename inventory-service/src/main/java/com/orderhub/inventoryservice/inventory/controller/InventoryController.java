package com.orderhub.inventoryservice.inventory.controller;

import com.orderhub.inventoryservice.inventory.dto.InventoryResponse;
import com.orderhub.inventoryservice.inventory.service.InventoryService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Envanter (Inventory) ile ilgili HTTP isteklerini karşılayan ve dış dünyaya
 * RESTful standartlarda API uç noktaları (endpoints) sunan Controller sınıfıdır.
 *
 * Temel Sorumlulukları:
 * - İstemciden (frontend / diğer mikro hizmetler) gelen HTTP GET isteklerini karşılamak.
 * - İş katmanından (`InventoryService`) gelen verileri uygun HTTP durum kodlarıyla (200 OK vb.) sarmalayarak dönmek.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        log.debug("Getting all inventory items");

        return new ResponseEntity<>(inventoryService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponse> getBySku(
            @PathVariable("sku")
            @Pattern(regexp = "^[A-Z0-9-]{3,32}$",
                    message = "SKU 3-32 karakter olmalı; sadece büyük harf, rakam ve tire içerebilir")
            String sku) {
        log.debug("Getting inventory with sku {}", sku);

        return new ResponseEntity<>(inventoryService.getBySku(sku), HttpStatus.OK);
    }
}
