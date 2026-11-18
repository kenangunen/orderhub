package com.orderhub.productservice.product.repository;

import com.orderhub.productservice.product.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product modülü için veri erişim sözleşmesini (repository interface) tanımlar.
 * İş katmanı ile veri tabanı/depolama katmanı arasında soyutlama (abstraction) sağlayarak
 * servis sınıfının alt seviye veri detaylarından bağımsız çalışmasını amaçlar.
 */
public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll();

    void deleteById(UUID id);

    Optional<Product> findBySku(String sku);
}
