package com.orderhub.orderservice.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    // TEST: fişteki örnek, 1500 × 2 = 3000
    @Test
    void lineTotal_validItem_returnsUnitPriceTimesQuantity() {
        OrderItem item = new OrderItem("KLV-0001", "Klavye", 2, new BigDecimal("1500"));

        // BigDecimal'de isEqualTo değil isEqualByComparingTo: 3000 ile 3000.00 eşit sayılsın
        assertThat(item.lineTotal()).isEqualByComparingTo("3000");
    }

    // TEST: boş sku ile satır oluşamaz
    @Test
    void constructor_blankSku_throwsIllegalArgument() {
        assertThatThrownBy(() -> new OrderItem(" ", "Klavye", 2, new BigDecimal("1500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sku");
    }

    // TEST: boş ürün adıyla satır oluşamaz (snapshot eksik kalmasın)
    @Test
    void constructor_blankProductName_throwsIllegalArgument() {
        assertThatThrownBy(() -> new OrderItem("KLV-0001", "", 2, new BigDecimal("1500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productName");
    }

    // TEST: sınır değer, adet 0 kabul edilmez
    @Test
    void constructor_zeroQuantity_throwsIllegalArgument() {
        assertThatThrownBy(() -> new OrderItem("KLV-0001", "Klavye", 0, new BigDecimal("1500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    // TEST: sınır değer, fiyat 0 kabul edilmez
    @Test
    void constructor_zeroUnitPrice_throwsIllegalArgument() {
        assertThatThrownBy(() -> new OrderItem("KLV-0001", "Klavye", 2, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitPrice");
    }

    // TEST: fiyatsız satır oluşamaz
    @Test
    void constructor_nullUnitPrice_throwsIllegalArgument() {
        assertThatThrownBy(() -> new OrderItem("KLV-0001", "Klavye", 2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitPrice");
    }
}