package com.orderhub.productservice.product.service;

import com.orderhub.productservice.exception.ProductNotFoundException;
import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

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

    public ProductResponse getById(UUID id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

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