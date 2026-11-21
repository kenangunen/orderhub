package com.orderhub.orderservice.order.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Order {

    @EqualsAndHashCode.Include
    @ToString.Exclude
    private final UUID id;

    @ToString.Include
    private final UUID customerId;

    // final değil: ileride confirm() / cancel() değiştirecek
    @ToString.Include
    private OrderStatus status;

    private final List<OrderItem> items;

    // Tüm kurallar burada: hangi yoldan gelinirse gelinsin geçersiz sipariş oluşmaz
    private Order(UUID id, UUID customerId, OrderStatus status, List<OrderItem> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId must not be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must have at least one item");
        }
        // Değiştirilemez kopya; içinde null satır varsa burada NullPointerException
        List<OrderItem> copy = List.copyOf(items);
        requireUniqueSkus(copy);

        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.items = copy;
    }

    // Tek giriş kapısı: id ve başlangıç durumu sipraiş kendisi belirler
    public static Order place(UUID customerId, List<OrderItem> items) {
        return new Order(UUID.randomUUID(), customerId, OrderStatus.PENDING, items);
    }

    // Fişin en altındaki toplam: satır toplamlarının toplamı
    public BigDecimal totalAmount() {
        return items.stream()
                .map(OrderItem::lineTotal)
                // 0'dan başla, her satır toplamını üstüne ekle
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Karar: Aynı SKU iki satırda olamaz, sessizce birleştirilmez.
    private static void requireUniqueSkus(List<OrderItem> items) {
        Set<String> seen = new HashSet<>();
        for (OrderItem item : items) {
            // Set.add(): eleman zaten varsa false döner
            if (!seen.add(item.sku())) {
                throw new IllegalArgumentException("duplicate sku in order: " + item.sku());
            }
        }
    }
}
