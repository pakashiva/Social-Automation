package com.shoppingmall.service.impl;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.dto.StockAdjustForm;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.AdjustmentDirection;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.AppSettingRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.InventoryService;
import com.shoppingmall.util.Pages;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Set<StockMovementType> SYSTEM = EnumSet.of(
            StockMovementType.OPENING,
            StockMovementType.SALE,
            StockMovementType.RESERVE,
            StockMovementType.RELEASE,
            StockMovementType.RETURN);

    private final OwnershipGuard guard;
    private final ShopRepository shops;
    private final InventoryRepository inventories;
    private final AppSettingRepository settings;
    private final StockLedger ledger;

    public InventoryServiceImpl(
            OwnershipGuard guard,
            ShopRepository shops,
            InventoryRepository inventories,
            AppSettingRepository settings,
            StockLedger ledger) {
        this.guard = guard;
        this.shops = shops;
        this.inventories = inventories;
        this.settings = settings;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.InventoryRow> listForOwner(long userId, int page) {
        List<Long> shopIds = shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return Page.empty(Pages.of(page));
        }
        int threshold = settings.findById(1L).orElseThrow().getLowStockThreshold();
        List<Rows.InventoryRow> rows = inventories.findForShops(shopIds).stream().map(inventory -> row(inventory, threshold)).toList();
        int from = Math.min(Math.max(page, 0) * Pages.SIZE, rows.size());
        int to = Math.min(from + Pages.SIZE, rows.size());
        return new PageImpl<>(rows.subList(from, to), Pages.of(page), rows.size());
    }

    @Override
    @Transactional
    public void adjust(long userId, long productId, StockAdjustForm form) {
        guard.requireOwnProduct(userId, productId);
        if (form.getMovementType() == null || SYSTEM.contains(form.getMovementType())) {
            throw new BusinessException("That movement type is system controlled.", "/owner/inventory");
        }
        if (form.getQuantity() == null || form.getQuantity() < 1) {
            throw new BusinessException("Quantity must be a positive whole number.", "/owner/inventory");
        }
        if (form.getNote() == null || form.getNote().trim().length() < 5 || form.getNote().trim().length() > 500) {
            throw new BusinessException("Write a note of at least 5 characters.", "/owner/inventory");
        }
        int delta = form.getQuantity();
        if (form.getMovementType() == StockMovementType.DAMAGE) {
            delta = -delta;
        } else if (form.getMovementType() == StockMovementType.ADJUSTMENT) {
            if (form.getDirection() == null) {
                throw new BusinessException("Choose a direction for an adjustment.", "/owner/inventory");
            }
            if (form.getDirection() == AdjustmentDirection.OUT) {
                delta = -delta;
            }
        } else if (form.getMovementType() != StockMovementType.PURCHASE) {
            throw new BusinessException("That movement type is system controlled.", "/owner/inventory");
        }
        Inventory inventory = inventories.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("Not enough stock to remove that many.", "/owner/inventory"));
        ledger.apply(inventory, delta, 0, form.getMovementType(), null, null, form.getNote().trim(), guard.requireUser(userId));
    }

    private static Rows.InventoryRow row(Inventory inventory, int threshold) {
        Rows.InventoryRow row = new Rows.InventoryRow();
        row.setProductId(inventory.getProduct().getId());
        row.setSku(inventory.getProduct().getSku());
        row.setName(inventory.getProduct().getName());
        row.setShopName(inventory.getProduct().getShop().getName());
        row.setOnHand(inventory.getQuantityOnHand());
        row.setReserved(inventory.getQuantityReserved());
        row.setLowStock(inventory.getQuantityOnHand() <= threshold);
        return row;
    }
}
