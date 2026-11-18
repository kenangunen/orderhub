package com.orderhub.productservice.product.dto;

import jakarta.validation.constraints.*;
import lombok.With;

import java.math.BigDecimal;


/**
 * Yeni bir ürün oluşturma isteğini (Create Product Request) temsil eden DTO sınıfı.
 * SKU, ürün adı, açıklama, fiyat ve kategori alanları için gerekli Jakarta Bean Validation kurallarını barındırır.
 */
@With
public record CreateProductRequest(

        @NotBlank(message = "SKU boş olamaz")
        @Pattern(regexp = SKU_REGEX, message = SKU_MESSAGE)   // kural tek yerde, controller da aynısını kullanıyor
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

        // Anotasyon parametresi derleme anı sabiti olmalı → static final String
        public static final String SKU_REGEX = "^[A-Z0-9-]{3,32}$";
        public static final String SKU_MESSAGE =
                "SKU 3-32 karakter olmalı; sadece büyük harf, rakam ve tire içerebilir";
}