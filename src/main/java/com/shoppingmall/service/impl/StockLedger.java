package com.shoppingmall.service.impl;

import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.StockMovement;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.AppSettingRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.StockMovementRepository;
import com.shoppingmall.service.NotificationService;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class StockLedger {

    private final InventoryRepository inventories;
    private final StockMovementRepository movements;
    private final AppSettingRepository settings;
    private final NotificationService notifications;
    private final Clock clock;

    public StockLedger(
            InventoryRepository inventories,
            StockMovementRepository movements,
            AppSettingRepository settings,
            NotificationService notifications,
            Clock clock) {
        this.inventories = inventories;
        this.movements = movements;
        this.settings = settings;
        this.notifications = notifications;
        this.clock = clock;
    }

    public void apply(
            Inventory inventory,
            int onHandDelta,
            int reservedDelta,
            StockMovementType type,
            String referenceType,
            Long referenceId,
            String note,
            User actor) {
        int before = inventory.getQuantityOnHand();
        int onHand = before + onHandDelta;
        int reserved = inventory.getQuantityReserved() + reservedDelta;
        if (onHand < 0 || reserved < 0) {
            throw new BusinessException("Not enough stock to remove that many.", "/owner/inventory");
        }
        inventory.setQuantityOnHand(onHand);
        inventory.setQuantityReserved(reserved);
        inventories.save(inventory);
        StockMovement movement = new StockMovement();
        movement.setInventory(inventory);
        movement.setProduct(inventory.getProduct());
        movement.setShop(inventory.getProduct().getShop());
        movement.setMovementType(type);
        movement.setQuantityDelta(onHandDelta);
        movement.setReservedDelta(reservedDelta);
        movement.setQuantityOnHandAfter(onHand);
        movement.setQuantityReservedAfter(reserved);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movement.setCreatedBy(actor);
        movement.setCreatedAt(LocalDateTime.now(clock));
        movements.save(movement);
        int threshold = settings.findById(1L).orElseThrow().getLowStockThreshold();
        if (before > threshold && onHand <= threshold) {
            notifications.notifyLowStock(inventory.getProduct(), onHand);
        }
    }
}
