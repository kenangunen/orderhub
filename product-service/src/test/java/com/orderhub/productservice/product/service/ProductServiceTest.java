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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Spring yok; Mockito, @Mock ve @InjectMocks alanlarını her testten önce yeniden oluşturur.
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    InventoryClient inventoryClient;       // sahte inventory client

    @Mock
    ProductRepository productRepository;   // sahte repository

    @InjectMocks
    ProductService productService;         // gerçek service, mock'lar constructor'a verilir

    // ---------- create ----------

    // TEST: create, ürünü request'teki bilgilerle kaydedip cevaba çeviriyor mu?
    @Test
    void create_savesProductAndReturnsResponse() {
        CreateProductRequest request = new CreateProductRequest(
                "KLV-0001", "Mekanik klavye", "TR Q, kahverengi switch",
                new BigDecimal("1899.90"), "Elektronik");

        // save'e gelen ürünü aynen geri dön
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(request);

        // save'e giden Product'ı yakala ve içine bak
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getSku()).isEqualTo("KLV-0001");
        assertThat(saved.getName()).isEqualTo("Mekanik klavye");
        assertThat(saved.getPrice()).isEqualByComparingTo("1899.90");  // BigDecimal: değere göre karşılaştır
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.sku()).isEqualTo("KLV-0001");
    }

    // TEST: create, geçersiz fiyatta exception fırlatıp hiçbir şey kaydetmiyor mu?
    @Test
    void create_invalidPrice_throwsAndDoesNotSave() {
        CreateProductRequest request = new CreateProductRequest(
                "KLV-0001", "Mekanik klavye", null, new BigDecimal("-5"), "Elektronik");

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");

        verify(productRepository, never()).save(any());   // save hiç çağrılmamalı
    }

    // ---------- getById (stoksuz) ----------

    // TEST: getById, var olan ürünü cevaba çevirip dönüyor mu?
    @Test
    void getById_existingProduct_returnsResponse() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(product.getId());

        assertThat(response.id()).isEqualTo(product.getId());
        assertThat(response.name()).isEqualTo("Mekanik klavye");
    }

    // TEST: getById, olmayan ürün için ProductNotFoundException fırlatıyor mu?
    @Test
    void getById_missingProduct_throwsProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(id.toString());     // mesajda id olmalı
    }

    // TEST: stok istenmeyen getById inventory'yi hiç çağırmaz, quantityOnHand null kalır
    @Test
    void getById_doesNotCallInventory() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(product.getId());

        assertThat(response.quantityOnHand()).isNull();
        // Task 16'nın kazancı: gereksiz HTTP çağrısı yok
        verify(inventoryClient, never()).getAvailableQty(anyString());
    }

    // ---------- getByIdWithInventory (stoklu) ----------

    // TEST: inventory stoğu bildiriyorsa stoklu detayda quantityOnHand dolu gelir
    @Test
    void getByIdWithInventory_inventoryAvailable_includesQuantity() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        // Sahte client: bu sku için 50 adet var
        when(inventoryClient.getAvailableQty(product.getSku())).thenReturn(Optional.of(50));

        ProductResponse response = productService.getByIdWithInventory(product.getId());

        assertThat(response.quantityOnHand()).isEqualTo(50);
    }

    // TEST: inventory'de kayıt yoksa ürün yine döner, quantityOnHand null (0 değil)
    @Test
    void getByIdWithInventory_noInventoryRecord_quantityIsNull() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryClient.getAvailableQty(product.getSku())).thenReturn(Optional.empty());

        ProductResponse response = productService.getByIdWithInventory(product.getId());

        assertThat(response.quantityOnHand()).isNull();
    }

    // TEST: inventory'ye ulaşılamazsa exception dışarı sızmaz; ürün döner, quantityOnHand null
    @Test
    void getByIdWithInventory_inventoryDown_returnsProductWithNullQuantity() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        // inventory çökmüş gibi davran: bağlantı hatası
        when(inventoryClient.getAvailableQty(product.getSku()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        ProductResponse response = productService.getByIdWithInventory(product.getId());

        // Ürün bilgisi eksiksiz geliyor, sadece stok bilinmiyor
        assertThat(response.sku()).isEqualTo(product.getSku());
        assertThat(response.quantityOnHand()).isNull();
    }

    // ---------- getInventory (sadece stok) ----------

    // TEST: stok varsa productId, sku ve adet dolu döner
    @Test
    void getInventory_inventoryAvailable_returnsQuantity() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryClient.getAvailableQty(product.getSku())).thenReturn(Optional.of(50));

        ProductInventoryResponse response = productService.getInventory(product.getId());

        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.sku()).isEqualTo("KLV-0001");
        assertThat(response.availableQty()).isEqualTo(50);
    }

    // TEST: ürün yoksa ProductNotFoundException; inventory'ye hiç gidilmez
    @Test
    void getInventory_missingProduct_throwsProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getInventory(id))
                .isInstanceOf(ProductNotFoundException.class);

        verify(inventoryClient, never()).getAvailableQty(anyString());   // gereksiz HTTP çağrısı yok
    }

    // TEST: stok kaydı yoksa InventoryNotFoundException (0 dönmez)
    @Test
    void getInventory_noInventoryRecord_throwsInventoryNotFound() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryClient.getAvailableQty(product.getSku())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getInventory(product.getId()))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("KLV-0001");
    }

    // TEST: inventory'ye ulaşılamazsa InventoryUnavailableException; asıl hata cause olarak taşınıyor
    @Test
    void getInventory_inventoryDown_throwsUnavailableWithCause() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryClient.getAvailableQty(product.getSku()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> productService.getInventory(product.getId()))
                .isInstanceOf(InventoryUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);   // exception chaining kanıtı
    }

    // ---------- getAll ----------

    // TEST: getAll, her ürünü cevaba çevirip sırayı koruyor mu?
    @Test
    void getAll_mapsEveryProductToResponse() {
        Product first = existingProduct();
        Product second = Product.create("MSE-0001", "Kablosuz mouse", null,
                new BigDecimal("599.00"), "Elektronik");
        when(productRepository.findAll()).thenReturn(List.of(first, second));

        List<ProductResponse> responses = productService.getAll(20);

        assertThat(responses)
                .extracting(ProductResponse::sku)           // listeden sadece sku'ları al
                .containsExactly("KLV-0001", "MSE-0001");   // bu sırayla
    }

    // ---------- update ----------

    // TEST: update, değişiklikleri ürüne uygulayıp kaydediyor mu?
    @Test
    void update_existingProduct_appliesChangesAndSaves() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        UpdateProductRequest request = new UpdateProductRequest(
                "Mekanik klavye Pro", "Kırmızı switch", new BigDecimal("2149.90"), "Elektronik");

        productService.update(product.getId(), request);

        // service aynı nesneyi değiştirdi, değişikliği burada görüyoruz
        assertThat(product.getName()).isEqualTo("Mekanik klavye Pro");
        assertThat(product.getPrice()).isEqualByComparingTo("2149.90");
        verify(productRepository).save(product);
    }

    // TEST: update, geçersiz fiyatta ürünü hiç değiştirmeden duruyor mu? (atomic güncelleme)
    @Test
    void update_invalidPrice_throwsAndLeavesProductUnchanged() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        UpdateProductRequest request = new UpdateProductRequest(
                "Bu ad kaydedilmemeli", null, new BigDecimal("-5"), "Elektronik");

        assertThatThrownBy(() -> productService.update(product.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(product.getName()).isEqualTo("Mekanik klavye");   // ad da değişmemiş olmalı
        verify(productRepository, never()).save(any());
    }

    // TEST: update, olmayan ürün için exception fırlatıp kaydetmiyor mu?
    @Test
    void update_missingProduct_throwsAndDoesNotSave() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        UpdateProductRequest request = new UpdateProductRequest(
                "Ad", null, new BigDecimal("10.00"), "Elektronik");

        assertThatThrownBy(() -> productService.update(id, request))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    // ---------- delete ----------

    // TEST: delete, var olan ürünü doğru id ile siliyor mu?
    @Test
    void delete_existingProduct_deletesById() {
        Product product = existingProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        productService.delete(product.getId());

        verify(productRepository).deleteById(product.getId());
    }

    // TEST: delete, olmayan ürün için exception fırlatıp silme yapmıyor mu?
    @Test
    void delete_missingProduct_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }

    // ---------- Helper ----------

    // Testlerde "repository'de zaten var olan" ürün
    private Product existingProduct() {
        return Product.create("KLV-0001", "Mekanik klavye", "TR Q, kahverengi switch",
                new BigDecimal("1899.90"), "Elektronik");
    }
}