package com.orderhub.productservice.product.repository;

import com.orderhub.productservice.product.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductRepository arayüzünün bellek üstü (in-memory) somut uygulamasıdır.
 * Ürün verilerini thread-safe yapıda bir ConcurrentHashMap içerisinde saklar
 * ve veri tabanı bağımlılığı olmaksızın hızlı test ve çalışma ortamı sunar.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    @Override
    public void deleteById(UUID id) {
        products.remove(id);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return products
                .values()
                .stream()
                .filter(product -> product.getSku().equals(sku))
                .findFirst();
    }
}