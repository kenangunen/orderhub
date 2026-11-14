package com.orderhub.productservice.product.dto;

import jakarta.validation.constraints.*;
import lombok.With;

import java.math.BigDecimal;


@With
public record CreateProductRequest(

        @NotBlank(message = "SKU boş olamaz")
        @Pattern(regexp = "^[A-Z0-9-]{3,32}$",
                message = "SKU 3-32 karakter olmalı; sadece büyük harf, rakam ve tire içerebilir")
        String sku,

        @NotBlank(message = "Ürün adı boş olamaz")
        @Size(max=120, message = "Ürün adı en fazla 120 karakter olabilir")
        String name,

        @Size(max=1000, message= "Açıklama en fazla 1000 karakter olabilir")
        String description,

        @NotNull(message = "Fiyat zorunludur")
        @Positive(message = "Fiyat sıfırdan büyük olmalı")
        @Digits(integer = 10, fraction = 2, message = "Fiyat en fazla 10 basamak ve 2 ondalık olabilir")
        BigDecimal price,

        @NotBlank(message = "Kategori boş olamaz")
        String category) {

}
