package com.orderhub.productservice.product.controller;

import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.verify;

// Sadece ProductController'ın web katmanını (URL eşleşmesi, JSON, status kodları) yükler; service ve repository yüklenmez.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    // Gerçek sunucu açmadan controller'a HTTP isteği atar ve cevabın status, header ve gövdesini kontrol etmeyi sağlar.
    @Autowired
    MockMvc mockMvc;

    // Uygulamanın kendi Jackson ayarlarıyla request record'larını POST/PUT gövdesi için JSON'a çevirir.
    @Autowired
    JsonMapper jsonMapper;

    // Controller'ın ihtiyaç duyduğu ProductService yerine context'e sahte bir nesne koyar; ne döneceğine her testte biz karar veririz.
    @MockitoBean
    ProductService productService;

    @Test
    void getById_returnsProduct() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(sampleResponse(id));

        // when + then
        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("KLV-0001"))
                .andExpect(jsonPath("$.price").value(1899.90));
    }

    @Test
    void getAll_returnsProducts() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(productService.getAll(20)).thenReturn(List.of(sampleResponse(id1), sampleResponse(id2)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id1.toString()));
    }

    @Test
    void create_returns201WithLocation() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "KLV-0001",
                "Mekanik klavye",
                "TR Q, kahverengi switch",
                new BigDecimal("1899.90"),
                "Elektronik");

        UUID id = UUID.randomUUID();

        when(productService.create(request)).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void delete_returns204() throws Exception {
        // given
        UUID id = UUID.randomUUID();

        // when + then
        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);
    }

    private ProductResponse sampleResponse(UUID id) {
        Instant now = Instant.parse("2026-11-12T09:00:00Z");
        return new ProductResponse(
                id,
                "KLV-0001",
                "Mekanik klavye",
                "TR Q, kahverengi switch",
                new BigDecimal("1899.90"),
                "Elektronik",
                true,
                now,
                now
        );
    }

    @Test
    void update_returns204() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Mekanik klavye Pro",
                "TR Q, kırmızı switch",
                new BigDecimal("2149.90"),
                "Elektronik");

        // when + then
        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(productService).update(id, request);
    }
}