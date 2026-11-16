package com.orderhub.productservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

// InventoryClient'ın RestClient ile yazılmış hali. Spring bu bean'i InventoryClient isteyen yere verecek.
@Component
@RequiredArgsConstructor
public class RestClientInventoryClient implements InventoryClient {

    // RestClientConfig'de kurduğumuz bean (adres + timeout'lar hazır)
    private final RestClient inventoryRestClient;

    @Override
    public Optional<Integer> getAvailableQty(String sku) {
        try {
            InventoryStockResponse response = inventoryRestClient.get()
                    // baseUrl'in üstüne eklenir; {sku} yerine değer güvenli şekilde konur (URL encode dahil)
                    .uri("/api/v1/inventory/{sku}", sku)
                    // İsteği gönder; 4xx/5xx gelirse exception fırlat
                    .retrieve()
                    // 200 gelirse JSON'u record'a çevir
                    .body(InventoryStockResponse.class);

            return Optional.ofNullable(response).map(InventoryStockResponse::availableQty);

        } catch (HttpClientErrorException.NotFound e){
            // 404 = bu sku için stok kaydı yok → hata değil, "bilgi yok"
            return Optional.empty();
        }
        // Diğer tüm hatalar (bağlantı, timeout, 5xx) burada yakalanmaz → çağırana fırlar
    }
}
