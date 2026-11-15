package com.orderhub.inventoryservice.inventory.service;

import com.orderhub.inventoryservice.exception.InventoryItemNotFoundException;
import com.orderhub.inventoryservice.inventory.InventoryItem;
import com.orderhub.inventoryservice.inventory.dto.InventoryResponse;
import com.orderhub.inventoryservice.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String SKU = "KLV-0001";
    private static final int AVAILABLE_QTY = 20;

    @Mock
    InventoryRepository inventoryRepository; // sahte repository

    @InjectMocks
    InventoryService inventoryService; // gerçek service, mock constructor'a verilir

    // TEST: sku varsa repository'deki kayıt DTO'ya çevrilip dönülür
    @Test
    void getBySku_existingSku_returnsResponse() {
        when(inventoryRepository.findBySku(SKU)).thenReturn(Optional.of(existingInventoryItem()));

        InventoryResponse response = inventoryService.getBySku(SKU);

        assertThat(response.sku()).isEqualTo(SKU);
        assertThat(response.availableQty()).isEqualTo(AVAILABLE_QTY);
        assertThat(response.reservedQty()).isZero();
    }

    // TEST: sku yoksa InventoryItemNotFoundException fırlar, mesajda sku geçer
    @Test
    void getBySku_missingSku_throwsNotFound() {
        String sku = "TEST-0000";
        when(inventoryRepository.findBySku(sku)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getBySku(sku))
                .isInstanceOf(InventoryItemNotFoundException.class)
                .hasMessageContaining(sku);
    }

    // TEST: getAll repository'deki her kaydı sırasıyla DTO'ya çevirir
    @Test
    void getAll_mapsEveryItemToResponse() {
        InventoryItem first = existingInventoryItem();
        InventoryItem second = InventoryItem.create("MSE-0001", 40);
        when(inventoryRepository.findAll()).thenReturn(List.of(first, second));

        List<InventoryResponse> response = inventoryService.getAll();

        assertThat(response)
                .extracting(InventoryResponse::sku)
                .containsExactly(SKU, "MSE-0001");
    }

    // Testlerde "repository'de zaten var olan" inventory item
    private InventoryItem existingInventoryItem() {
        return InventoryItem.create(SKU, AVAILABLE_QTY);
    }
}