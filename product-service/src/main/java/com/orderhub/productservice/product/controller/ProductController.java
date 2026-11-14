package com.orderhub.productservice.product.controller;

import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RequestMapping("/api/v1/products")
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(name = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit en az 1 olmalı")
            @Max(value = 100, message = "limit en fazla 100 olabilir")
            int limit
    ) {
        log.debug("Getting all products, limit: {}", limit);

        return new ResponseEntity<>(productService.getAll(limit), HttpStatus.OK);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getById(@PathVariable("productId") UUID productId) {
        log.debug("REST request to get product with id: {}", productId);

        return new ResponseEntity<>(productService.getById(productId), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest createProductRequest) {
        log.debug("REST request to create product : {}", createProductRequest);

        ProductResponse created = productService.create(createProductRequest);
        URI location = URI.create("/api/v1/products/" + created.id());

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("productId") UUID productId, @Valid @RequestBody UpdateProductRequest updateProductRequest) {
        log.debug("REST request to update product : {}", updateProductRequest);

        productService.update(productId, updateProductRequest);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("productId") UUID productId) {
        log.debug("REST request to delete product : {}", productId);

        productService.delete(productId);
    }
}
