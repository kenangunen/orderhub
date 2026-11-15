package com.orderhub.inventoryservice.inventory.bootstrap;

import com.orderhub.inventoryservice.inventory.InventoryItem;
import com.orderhub.inventoryservice.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class InventoryDataLoader implements ApplicationRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!inventoryRepository.findAll().isEmpty()) {
            log.info("Inventory items already exist, skipping bootstrap");
            return;
        }

        List<InventoryItem> items = List.of(
                InventoryItem.create("KLV-0001", 50),
                InventoryItem.create("MSE-0001", 0),
                InventoryItem.create("MNT-0001", 12)
        );

        items.forEach(inventoryRepository::save);

        log.info("Loaded {} Inventory items for dev profile", items.size());
    }
}

