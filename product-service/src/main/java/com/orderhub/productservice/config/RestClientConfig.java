package com.orderhub.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// @Configuration: içindeki @Bean metotlarının döndürdüğü nesneleri Spring bean olarak kaydeder
@Configuration
public class RestClientConfig {

    // Spring açılışta bu metodu bir kez çağırır; dönen RestClient'ı bean olarak saklar.
    // Parametre (InventoryClientProperties) Spring tarafından inject edilir → az önce yaml'dan doldurulan nesne
    @Bean
    RestClient inventoryRestClient(InventoryClientProperties properties) {
        // Timeout'lar RestClient'ın kendisinde değil, isteği fiilen gönderen "request factory"de ayarlanır
        // Request factory; RestClient isteğin ne olduğunu tarif eder
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
