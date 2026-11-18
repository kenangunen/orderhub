package com.orderhub.productservice.product.service;

import com.orderhub.productservice.client.InventoryClient;
import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.*;

// Cache bir proxy davranışı; Mockito tek başına göremez, gerçek Spring context gerekiyor.
@SpringBootTest
class ProductCacheTest {

    @Autowired
    ProductService productService;         // proxy'li gerçek service

    @Autowired
    CacheManager cacheManager;             // Boot'un kurduğu CaffeineCacheManager

    @MockitoBean
    ProductRepository productRepository;   // context'teki InMemory repository'nin yerine geçer

    @MockitoBean
    InventoryClient inventoryClient;       // gerçek HTTP çağrısı yapılmasın

    @BeforeEach
    void clearCaches() {
        // context testler arasında paylaşılıyor; cache'i temizlemezsek bir test diğerinin verisini görür
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    // ---------- getById ----------

    // TEST: aynı id ikinci kez istenince repository'ye gidilmiyor (cache hit)
    @Test
    void getById_calledTwice_repositoryHitOnce() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        productService.getById(product.getId());   // miss: metot çalışır, sonuç cache'e yazılır
        productService.getById(product.getId());   // hit: metot hiç çalışmaz

        verify(productRepository, times(1)).findById(product.getId());
    }

    // ---------- getByIdWithInventory ----------

    // TEST: stoklu okuma cache'lenmiyor; inventory her istekte çağrılıyor
    @Test
    void getByIdWithInventory_calledTwice_inventoryHitTwice() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryClient.getAvailableQty(product.getSku())).thenReturn(Optional.of(50));

        productService.getByIdWithInventory(product.getId());
        productService.getByIdWithInventory(product.getId());

        verify(inventoryClient, times(2)).getAvailableQty(product.getSku());
    }

    // ---------- update ----------

    // TEST: update sonrası getById cache'ten değil repository'den okuyor (evict çalıştı)
    @Test
    void update_evictsProductCache() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        productService.getById(product.getId());   // cache doldu

        productService.update(product.getId(), new UpdateProductRequest(
                "Mekanik klavye Pro", "Kırmızı switch", new BigDecimal("2149.90"), "Elektronik"));
        clearInvocations(productRepository);       // buraya kadarki çağrıları sıfırla

        productService.getById(product.getId());

        verify(productRepository, times(1)).findById(product.getId());   // cache boştu, kaynağa gitti
    }

    // ---------- getBySku ----------

    // TEST: aynı sku ikinci kez istenince repository'ye gidilmiyor (cache hit)
    @Test
    void getBySku_calledTwice_repositoryHitOnce() {
        Product product = existingProduct();
        when(productRepository.findBySku(product.getSku())).thenReturn(Optional.of(product));

        productService.getBySku(product.getSku());   // miss: metot çalışır, sonuç productsBySku'ya yazılır
        productService.getBySku(product.getSku());   // hit: metot çalışmaz

        verify(productRepository, times(1)).findBySku(product.getSku());
    }

    // TEST: update sonrası getBySku cache'ten değil repository'den okuyor (allEntries evict çalıştı)
    @Test
    void update_evictsSkuCache() {
        Product product = existingProduct();
        when(productRepository.findBySku(product.getSku())).thenReturn(Optional.of(product));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));   // update ürünü id ile buluyor
        productService.getBySku(product.getSku());   // sku cache'i doldu

        productService.update(product.getId(), new UpdateProductRequest(
                "Mekanik klavye Pro", "Kırmızı switch", new BigDecimal("2149.90"), "Elektronik"));
        clearInvocations(productRepository);         // buraya kadarki çağrıları sıfırla

        productService.getBySku(product.getSku());

        verify(productRepository, times(1)).findBySku(product.getSku());   // cache boştu, kaynağa gitti
    }

    // ---------- Helper ----------

    // ProductServiceTest'teki ile aynı ürün
    private Product existingProduct() {
        return Product.create("KLV-0001", "Mekanik klavye", "TR Q, kahverengi switch",
                new BigDecimal("1899.90"), "Elektronik");
    }
}