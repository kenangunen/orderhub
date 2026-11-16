package com.orderhub.productservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// prefix: yaml'daki "orderhub.inventory" bloğunu bu record'a bağla
// Alan adları yaml anahtarlarıyla eşleşir: base-url → baseUrl (Spring tireyi kendisi çevirir)
@ConfigurationProperties(prefix = "orderhub.inventory")
public record InventoryClientProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
