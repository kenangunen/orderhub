package com.orderhub.productservice.product.controller;

import com.orderhub.productservice.exception.InventoryNotFoundException;
import com.orderhub.productservice.exception.InventoryUnavailableException;
import com.orderhub.productservice.exception.ProductNotFoundException;
import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductInventoryResponse;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    // ---------- GET /{id} ----------

    // TEST: var olan ürün için 200 ve ürün alanları döner
    @Test
    void getById_returnsProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("KLV-0001"))
                .andExpect(jsonPath("$.price").value(1899.90));
    }

    // TEST: var olan sku için 200 ve ürün JSON'u dönüyor
    @Test
    void getBySku_existingSku_returns200() throws Exception {
        ProductResponse response = ProductResponse.from(Product.create(
                "KLV-0001", "Mekanik klavye", null, new BigDecimal("1899.90"), "Elektronik"));
        when(productService.getBySku("KLV-0001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "KLV-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("KLV-0001"))
                .andExpect(jsonPath("$.name").value("Mekanik klavye"));
    }

    // TEST: olmayan sku 404 ve ProblemDetail dönüyor, detail'da sku geçiyor
    @Test
    void getBySku_missingSku_returns404() throws Exception {
        when(productService.getBySku("KLV-9999"))
                .thenThrow(ProductNotFoundException.forSku("KLV-9999"));

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "KLV-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"))
                .andExpect(jsonPath("$.detail").value(containsString("KLV-9999")));
    }

    // TEST: formata uymayan sku 400 alıyor ve service hiç çağrılmıyor
    @Test
    void getBySku_invalidFormat_returns400AndSkipsService() throws Exception {
        mockMvc.perform(get("/api/v1/products/sku/{sku}", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))   // Adım 6'nın koruması
                .andExpect(jsonPath("$.errors.sku").exists());

        verify(productService, never()).getBySku(anyString());   // validation controller'a girmeden durdurdu
    }

    // ---------- GET /{productId}/inventory ----------

    // TEST: stok varsa 200 ve JSON alanları dolu
    @Test
    void getInventory_existingProduct_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getInventory(id))
                .thenReturn(new ProductInventoryResponse(id, "KLV-0001", 50));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("KLV-0001"))
                .andExpect(jsonPath("$.availableQty").value(50));
    }

    // TEST: ürün yoksa 404 "Product Not Found"
    @Test
    void getInventory_missingProduct_returns404ProductNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getInventory(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"));
    }

    // TEST: stok kaydı yoksa 404 ama başlık farklı: "Inventory Not Found"
    @Test
    void getInventory_noInventoryRecord_returns404InventoryNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getInventory(id)).thenThrow(new InventoryNotFoundException("KLV-0001"));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Inventory Not Found"));
    }

    // TEST: inventory kapalıysa 503 + Retry-After; iç hata mesajı istemciye sızmıyor
    @Test
    void getInventory_inventoryDown_returns503WithRetryAfter() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getInventory(id)).thenThrow(new InventoryUnavailableException(
                "KLV-0001", new ResourceAccessException("Connection refused")));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", id))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.title").value("Inventory Unavailable"))
                .andExpect(jsonPath("$.detail").value(not(containsString("Connection refused"))));
    }

    // TEST: parametresiz istek stoksuz metodu çağırır; quantityOnHand JSON'da hiç yok
    @Test
    void getById_withoutShowInventory_omitsQuantity() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                // NON_NULL: değer null → alan JSON'a yazılmadı
                .andExpect(jsonPath("$.quantityOnHand").doesNotExist());

        verify(productService, never()).getByIdWithInventory(any());
    }

    // TEST: ?showInventory=true stoklu metodu çağırır; quantityOnHand JSON'da görünür
    @Test
    void getById_withShowInventory_includesQuantity() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getByIdWithInventory(id)).thenReturn(sampleResponse(id, 50));

        mockMvc.perform(get("/api/v1/products/{id}", id).param("showInventory", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(50));

        verify(productService, never()).getById(any());
    }

    // TEST: Service ProductNotFoundException fırlatırsa 404 ProblemDetail döner
    @Test
    void getById_missingProduct_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Product Not Found"))
                .andExpect(jsonPath("$.detail").value("Product not found: " + id));
    }

    // TEST: id UUID değilse service'e gelmeden 400 (tip dönüşüm hatası)
    @Test
    void getById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/abc"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getById(any());
    }

    // ---------- GET (liste) ----------

    // TEST: liste endpoint'i varsayılan limit (20) ile service'ten gelen ürünleri döner
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

    // TEST: limit verilirse service'e aynen iletilir
    @Test
    void getAll_withLimit_passesLimitToService() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("limit", "5"))
                .andExpect(status().isOk());

        verify(productService).getAll(eq(5));
    }

    // TEST: limit 100'ü aşarsa method validation → 400
    @Test
    void getAll_limitTooHigh_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("limit", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limit").value("limit en fazla 100 olabilir"));

        verify(productService, never()).getAll(anyInt());
    }

    // ---------- POST ----------

    // TEST: geçerli istek 201 + Location header + oluşan ürün döner
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

    // TEST: İsim boşsa 400 + ProblemDetail döner, service hiç çağrılmaz
    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        CreateProductRequest request = validCreateRequest().withName(""); // tek alanı boz

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.name").value("Ürün adı boş olamaz"));

        verify(productService, never()).create(any());   // validation metottan önce durdurdu
    }

    // TEST: Birden fazla hatalı alan varsa hepsi tek cevapta döner
    @Test
    void create_multipleInvalidFields_returnsAllErrors() throws Exception {
        CreateProductRequest request = validCreateRequest()
                .withName("")
                .withPrice(new BigDecimal("-5"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    // ---------- PUT ----------

    // TEST: geçerli güncelleme 204 döner ve service doğru id + istekle çağrılır
    @Test
    void update_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Mekanik klavye Pro",
                "TR Q, kırmızı switch",
                new BigDecimal("2149.90"),
                "Elektronik");

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(productService).update(id, request);
    }

    // TEST: PUT'ta da @Valid çalışır; fiyat 0 → 400, update çağrılmaz
    @Test
    void update_zeroPrice_returns400() throws Exception {
        UpdateProductRequest request = validUpdateRequest().withPrice(BigDecimal.ZERO);

        mockMvc.perform(put("/api/v1/products/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("Fiyat sıfırdan büyük olmalı"));

        verify(productService, never()).update(any(), any());
    }

    // ---------- DELETE ----------

    // TEST: silme 204 döner ve service doğru id ile çağrılır
    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);
    }

    // ---------- Helper'lar ----------

    // Stok bilgisi olmayan örnek cevap; mevcut testler bunu kullanır
    private ProductResponse sampleResponse(UUID id) {
        return sampleResponse(id, null);
    }

    // Stok bilgisiyle örnek cevap; showInventory testleri için
    private ProductResponse sampleResponse(UUID id, Integer quantityOnHand) {
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
                now,
                quantityOnHand
        );
    }

    // Tüm kuralları geçen istek; testlerde withX(...) ile tek alanı bozarız
    private CreateProductRequest validCreateRequest() {
        return new CreateProductRequest("SKU-001", "Kalem", "Mavi tükenmez",
                new BigDecimal("10.00"), "Kırtasiye");
    }

    private UpdateProductRequest validUpdateRequest() {
        return new UpdateProductRequest("Kalem", "Mavi tükenmez",
                new BigDecimal("10.00"), "Kırtasiye");
    }
}