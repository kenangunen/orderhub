package com.orderhub.inventoryservice.inventory.controller;

import com.orderhub.inventoryservice.exception.InventoryItemNotFoundException;
import com.orderhub.inventoryservice.inventory.dto.InventoryResponse;
import com.orderhub.inventoryservice.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    private static final String SKU = "KLV-0001";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InventoryService inventoryService;

    // TEST: var olan sku için 200 ve stok bilgisi döner
    @Test
    void getBySku_existingSku_returns200() throws Exception {
        when(inventoryService.getBySku(SKU)).thenReturn(sampleResponse(SKU, 20));

        mockMvc.perform(get("/api/v1/inventory/{sku}", SKU))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(SKU))
                .andExpect(jsonPath("$.availableQty").value(20))
                .andExpect(jsonPath("$.reservedQty").value(0));
    }

    // TEST: olmayan sku için 404 ProblemDetail döner
    @Test
    void getBySku_missingSku_returns404() throws Exception {
        String sku = "XYZ-999";
        when(inventoryService.getBySku(sku)).thenThrow(new InventoryItemNotFoundException(sku));

        mockMvc.perform(get("/api/v1/inventory/{sku}", sku))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Inventory Item Not Found"))
                .andExpect(jsonPath("$.detail").value("Inventory item not found: " + sku));
    }

    // TEST: 3 karakterden kısa sku validation'a takılır → 400, service çağrılmaz
    @Test
    void getBySku_tooShortSku_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{sku}", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.sku").exists());

        verify(inventoryService, never()).getBySku(any());
    }

    // TEST: küçük harfli sku regex'e uymaz → 400
    @Test
    void getBySku_lowercaseSku_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{sku}", "sku-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists());

        verify(inventoryService, never()).getBySku(any());
    }

    // TEST: liste endpoint'i service'ten gelen tüm kayıtları döner
    @Test
    void getAll_returnsItems() throws Exception {
        when(inventoryService.getAll()).thenReturn(List.of(
                sampleResponse(SKU, 20),
                sampleResponse("MSE-0001", 40)));

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sku").value(SKU))
                .andExpect(jsonPath("$[1].sku").value("MSE-0001"));
    }

    private InventoryResponse sampleResponse(String sku, int availableQty) {
        return new InventoryResponse(sku, availableQty, 0, Instant.parse("2026-11-15T09:00:00Z"));
    }
}