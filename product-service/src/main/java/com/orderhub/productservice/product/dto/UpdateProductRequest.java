package com.orderhub.productservice.product.dto;

import java.math.BigDecimal;

public record UpdateProductRequest(String name, String description, BigDecimal price, String category) {
}
