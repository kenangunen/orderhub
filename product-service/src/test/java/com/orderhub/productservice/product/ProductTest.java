package com.orderhub.productservice.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {
    @Test
    void equals_sameSku_differentName_areEqual() {
        Product a = product("KLV-0001", "Mekanik Klavye");
        Product b = product("KLV-0001", "RGB Klavye");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentSku_areNotEqual() {
        Product a = product("KLV-0001", "Mekanik klavye");
        Product b = product("KLV-0002", "Mekanik klavye");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_null_and_otherType_returnFalse() {
        Product a = product("KLV-0001", "Mekanik klavye");

        //noinspection ConstantValue
        assertThat(a.equals(null)).isFalse();
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(a.equals("KLV-0001")).isFalse();
    }

    @Test
    void equals_sameInstance_isTrue() {
        Product a = product("KLV-0001", "Mekanik klavye");

        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void hashSet_keepsOnlyOne_forSameSku() {
        Product a = product("KLV-0001", "Mekanik klavye");
        Product b = product("KLV-0001", "Başka ad");

        Set<Product> set = new HashSet<>();
        set.add(a);
        set.add(b);

        assertThat(set).hasSize(1);
    }

    @Test
    void hashSet_findsProduct_afterRename() {
        Product a = product("KLV-0001", "Mekanik klavye");
        Set<Product> set = new HashSet<>();
        set.add(a);

        a.rename("Yeni ad");

        assertThat(set).contains(a);
    }

    @Test
    void toString_containsSku() {
        Product a = product("KLV-0001", "Mekanik klavye");

        assertThat(a.toString()).contains("KLV-0001");
    }

    private Product product(String sku, String name) {
        return Product.create(sku, name, null, new BigDecimal("1899.90"), "Electronic");
    }
}