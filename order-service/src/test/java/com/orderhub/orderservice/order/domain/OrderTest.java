package com.orderhub.orderservice.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    // Sabit müşteri: her çalıştırmada aynı, test çıktısı okunabilir
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // Fişteki satırlar: 1500 × 2 = 3000 ve 800 × 1 = 800
    private static OrderItem keyboard() {
        return new OrderItem("KLV-0001", "Klavye", 2, new BigDecimal("1500"));
    }

    private static OrderItem mouse() {
        return new OrderItem("MSE-0001", "Mouse", 1, new BigDecimal("800"));
    }

    // TEST: fişin en altı, 3000 + 800 = 3800
    @Test
    void totalAmount_twoItems_returnsSumOfLineTotals() {
        Order order = Order.place(CUSTOMER_ID, List.of(keyboard(), mouse()));

        assertThat(order.totalAmount()).isEqualByComparingTo("3800");
    }

    // TEST: yeni sipariş id'sini ve PENDING durumunu kendisi alır
    @Test
    void place_validItems_createsPendingOrderWithId() {
        Order order = Order.place(CUSTOMER_ID, List.of(keyboard(), mouse()));

        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).containsExactly(keyboard(), mouse());
    }

    // TEST: boş fiş sipariş değildir
    @Test
    void place_emptyItems_throwsIllegalArgument() {
        assertThatThrownBy(() -> Order.place(CUSTOMER_ID, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    // TEST: müşterisiz sipariş olmaz
    @Test
    void place_nullCustomerId_throwsIllegalArgument() {
        assertThatThrownBy(() -> Order.place(null, List.of(keyboard())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");
    }

    // TEST: karar 4, aynı SKU iki satırda → reddedilir ve mesajda hangi SKU olduğu yazar
    @Test
    void place_duplicateSku_throwsWithSkuInMessage() {
        OrderItem keyboardAgain = new OrderItem("KLV-0001", "Klavye", 1, new BigDecimal("1500"));

        assertThatThrownBy(() -> Order.place(CUSTOMER_ID, List.of(keyboard(), mouse(), keyboardAgain)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KLV-0001");
    }

    // TEST: kapı kapalı, getItems() ile alınan listeye ekleme yapılamaz
    @Test
    void getItems_addAttempt_throwsUnsupportedOperation() {
        // Bilerek ArrayList: korumanın List.of'tan değil bizim kopyamızdan geldiğini görmek için
        List<OrderItem> source = new ArrayList<>(List.of(keyboard()));
        Order order = Order.place(CUSTOMER_ID, source);

        assertThatThrownBy(() -> order.getItems().add(mouse()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // TEST: defensive copy, dışarıdaki liste sonradan değişse de sipariş etkilenmez
    @Test
    void place_sourceListChangedAfterwards_orderUnaffected() {
        List<OrderItem> source = new ArrayList<>(List.of(keyboard(), mouse()));
        Order order = Order.place(CUSTOMER_ID, source);

        // Kapıdan geçmeden, aynı SKU'yu arka kapıdan sokmaya çalışıyoruz
        source.add(new OrderItem("KLV-0001", "Klavye", 5, new BigDecimal("1500")));

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.totalAmount()).isEqualByComparingTo("3800");
    }
}