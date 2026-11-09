package com.orderhub.productservice.product.service;

import com.orderhub.productservice.exception.ProductNotFoundException;
import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        Product product = Product.create(
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.category()
        );
        Product saved = productRepository.save(product);
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
    }

    public void delete(UUID id) {
        findProductOrThrow(id);
        productRepository.deleteById(id);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}