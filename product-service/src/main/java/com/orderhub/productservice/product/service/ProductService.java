package com.orderhub.productservice.product.service;

import com.orderhub.productservice.client.InventoryClient;
import com.orderhub.productservice.exception.InventoryNotFoundException;
import com.orderhub.productservice.exception.InventoryUnavailableException;
import com.orderhub.productservice.exception.ProductNotFoundException;
import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductInventoryResponse;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * Ürün (Product) yönetim süreçlerine ait iş kurallarını (business logic) barındıran servis sınıfıdır.
 * Ürün oluşturma, güncelleme, silme ve listeleme operasyonlarını yürütürken;
 * Caffeine Cache entegrasyonu (özellikle @Cacheable ve @CacheEvict yapıları) ile performans optimizasyonu sağlar
 * ve harici servislerle (InventoryClient) iletişim süreçlerini yönetir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    // Service sadece arayüzü biliyor; Spring RestClientInventoryClient'ı verecek
    private final InventoryClient inventoryClient;

    @CacheEvict(cacheNames = "productLists", allEntries = true) // yeni ürün tüm listeleri eskitir
    public ProductResponse create(CreateProductRequest request) {
        Product product = Product.create(
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.category()
        );
        Product saved = productRepository.save(product);

        log.info("Created product with id {}", saved.getId());

        return ProductResponse.from(saved);
    }

    // Sadece ürün bilgisi: inventory çağrılmaz
    @Cacheable(cacheNames = "products", key = "#id")  // ilk çağrı metodu çalıştırır, sonrakiler cache'ten döner
    public ProductResponse getById(UUID id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    // DEĞİŞTİ: geri geldi (16a). Ürün + stok: sadece istemci istediğinde (showInventory=true) çağrılır.
    // Cache yok: stok her istekte taze. Inventory'ye ulaşılamazsa ürün yine döner, adet null.
    public ProductResponse getByIdWithInventory(UUID id) {
        Product product = findProductOrThrow(id);
        return ProductResponse.from(product, fetchQuantityOnHand(product.getSku()));
    }

    @Cacheable(cacheNames = "productsBySku", key = "#sku")
    public ProductResponse getBySku(String sku) {
        log.debug("Getting product by sku {}", sku);

        return productRepository.findBySku(sku)
                .map(ProductResponse::from)
                .orElseThrow(() -> ProductNotFoundException.forSku(sku));
    }

    // DEĞİŞTİ: yorum kendi metodunu anlatıyor.
    // Sadece stok (GET /{id}/inventory). Cache yok (ADR-0005). Inventory hatası yutulmaz, çevrilir.
    public ProductInventoryResponse getInventory(UUID id) {
        Product product = findProductOrThrow(id);                                // ortak yardımcı → 404 Product Not Found

        try {
            int availableQty = inventoryClient.getAvailableQty(product.getSku())
                    .orElseThrow(() -> new InventoryNotFoundException(product.getSku()));   // kayıt yok → 404 Inventory Not Found
            return new ProductInventoryResponse(product.getId(), product.getSku(), availableQty);
        } catch (RestClientException ex) {                                      // bağlantı/timeout/5xx
            log.warn("Inventory unavailable for sku {}, responding 503: {}", product.getSku(), ex.getMessage());
            throw new InventoryUnavailableException(product.getSku(), ex);     // yutmuyoruz, çeviriyoruz → 503
        }
    }

    // Stok ek bilgi: inventory'ye ulaşılamazsa ürün yine döner, stok "bilinmiyor" (null) olur
    private Integer fetchQuantityOnHand(String sku) {
        try {
            return inventoryClient.getAvailableQty(sku).orElse(null);
        } catch (RestClientException e) {
            // Bağlantı hatası, timeout, 5xx → hepsi RestClientException'ın alt sınıfı
            log.warn("Inventory unavailable for sku {}, returning product without quantity: {}", sku, e.getMessage());
            return null;
        }
    }

    @Cacheable(cacheNames = "productLists", key = "#limit")   // DEĞİŞTİ: "productList" → "productLists" (yaml ve evict'lerle aynı)
    public List<ProductResponse> getAll(int limit) {
        return productRepository.findAll().stream()
                .limit(limit) // en fazla limit kadar ürün
                .map(ProductResponse::from)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "products", key = "#id"),
            @CacheEvict(cacheNames = "productLists", allEntries = true),
            @CacheEvict(cacheNames = "productsBySku", allEntries = true)
    })
    public void update(UUID id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);
        product.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.category()
        );
        productRepository.save(product);

        log.info("Updated product with id {}", product.getId());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "products", key = "#id"),
            @CacheEvict(cacheNames = "productLists", allEntries = true),
            @CacheEvict(cacheNames = "productsBySku", allEntries = true)
    })
    public void delete(UUID id) {
        findProductOrThrow(id);
        productRepository.deleteById(id);

        log.info("Deleted product with id {}", id);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}