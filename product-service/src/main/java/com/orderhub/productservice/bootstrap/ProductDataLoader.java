package com.orderhub.productservice.bootstrap;

import com.orderhub.productservice.product.Product;
import com.orderhub.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Uygulama açılırken örnek ürünleri yükler.
// @Profile("dev"): sadece dev profili aktifken bean oluşur → production ve testlerde çalışmaz
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class ProductDataLoader implements ApplicationRunner {

    private final ProductRepository productRepository;

    // ApplicationRunner: context tamamen ayağa kalktıktan sonra, istek kabul edilmeden önce bir kez çalışır
    @Override
    public void run(@NonNull ApplicationArguments args) {
        // Tekrar yükleme koruması: veri varsa dokunma (JPA'ya geçince kayıtlar kalıcı olacak)
        if (!productRepository.findAll().isEmpty()) {
            log.info("Products already exist, skipping bootstrap");
            return;
        }

        // Nesneler factory ile oluşturulur → invariant'lar burada da kontrol edilir
        // SKU'lar inventory-service'teki kayıtlarla aynı olmalı
        List<Product> products = List.of(
                Product.create("KLV-0001", "Mekanik Klavye", "RGB aydınlatmalı mekanik klavye",
                        new BigDecimal("1499.90"), "Elektronik"),
                Product.create("MSE-0001", "Kablosuz Mouse", "Şarj edilebilir kablosuz mouse",
                        new BigDecimal("549.90"), "Elektronik"),
                Product.create("MNT-0001", "27\" Monitör", "27 inç IPS monitör",
                        new BigDecimal("8999.00"), "Elektronik")
        );

        products.forEach(productRepository::save);

        log.info("Loaded {} products for dev profile", products.size());
    }
}