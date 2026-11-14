package com.orderhub.productservice.product.controller;

import com.orderhub.productservice.product.dto.CreateProductRequest;
import com.orderhub.productservice.product.dto.ProductResponse;
import com.orderhub.productservice.product.dto.UpdateProductRequest;
import com.orderhub.productservice.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequestMapping("/api/v1/products")
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        log.info("Getting all products");

        return new ResponseEntity<>(productService.getAll(), HttpStatus.OK);
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
