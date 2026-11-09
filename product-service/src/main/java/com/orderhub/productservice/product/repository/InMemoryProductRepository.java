package com.orderhub.productservice.product.repository;

import com.orderhub.productservice.product.Product;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
}