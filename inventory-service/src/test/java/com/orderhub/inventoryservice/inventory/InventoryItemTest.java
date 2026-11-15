package com.orderhub.inventoryservice.inventory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    private static final String SKU = "KLV-0001";

    // TEST: create verilen sku ve adedi atar, reservedQty 0 başlar, updatedAt dolu olur
    @Test
    void create_validInput_setsFieldsAndZeroReserved() {
        InventoryItem item = inventoryItem(SKU, 40);

        assertThat(item.getSku()).isEqualTo(SKU);
        assertThat(item.getAvailableQty()).isEqualTo(40);
        assertThat(item.getReservedQty()).isZero();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    // TEST: 0 adet geçerlidir (stokta yok); requirePositive hatasının regresyon testi
    @Test
    void create_zeroAvailableQty_isAllowed() {
        InventoryItem item = inventoryItem(SKU, 0);

        assertThat(item.getAvailableQty()).isZero();
    }

    // TEST: negatif availableQty ile nesne oluşturulamaz
    @Test
    void create_negativeAvailableQty_throws() {
        assertThatThrownBy(() -> inventoryItem(SKU, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("availableQty");
    }

    // TEST: boş veya sadece boşluktan oluşan sku reddedilir
    @Test
    void create_blankSku_throws() {
        assertThatThrownBy(() -> inventoryItem("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sku");

        assertThatThrownBy(() -> inventoryItem("   ", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sku");
    }

    // TEST: null sku reddedilir
    @Test
    void create_nullSku_throws() {
        assertThatThrownBy(() -> inventoryItem(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sku");
    }

    // TEST: kimlik sku'dur; adetler farklı olsa da aynı sku'lu iki kayıt eşittir
    @Test
    void equals_sameSkuDifferentQty_areEqual() {
        InventoryItem first = inventoryItem(SKU, 10);
        InventoryItem second = inventoryItem(SKU, 99);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private InventoryItem inventoryItem(String sku, int quantity) {
        return InventoryItem.create(sku, quantity);
    }
}