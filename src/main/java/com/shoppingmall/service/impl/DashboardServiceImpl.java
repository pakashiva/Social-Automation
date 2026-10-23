package com.shoppingmall.service.impl;

import com.shoppingmall.dto.DashboardView;
import com.shoppingmall.entity.ActivityLog;
import com.shoppingmall.entity.Cart;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.CartStatus;
import com.shoppingmall.enums.ComplaintStatus;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.ParkingSlotStatus;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.enums.ProductStatus;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.repository.ActivityLogRepository;
import com.shoppingmall.repository.AppSettingRepository;
import com.shoppingmall.repository.CartRepository;
import com.shoppingmall.repository.ComplaintRepository;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.NotificationRepository;
import com.shoppingmall.repository.ParkingSlotRepository;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.repository.ProductRepository;
import com.shoppingmall.repository.ShopOwnerRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.repository.WishlistRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.DashboardService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.TimeDisplay;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final OwnershipGuard guard;
    private final CartRepository carts;
    private final WishlistRepository wishlists;
    private final NotificationRepository notifications;
    private final MallOrderRepository orders;
    private final OrderMapper orderMapper;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final InventoryRepository inventories;
    private final AppSettingRepository settings;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final ShopOwnerRepository owners;
    private final ComplaintRepository complaints;
    private final ParkingSlotRepository slots;
    private final ActivityLogRepository activity;
    private final java.time.Clock clock;

    public DashboardServiceImpl(
            OwnershipGuard guard,
            CartRepository carts,
            WishlistRepository wishlists,
            NotificationRepository notifications,
            MallOrderRepository orders,
            OrderMapper orderMapper,
            ShopRepository shops,
            ProductRepository products,
            InventoryRepository inventories,
            AppSettingRepository settings,
            PaymentRepository payments,
            UserRepository users,
            ShopOwnerRepository owners,
            ComplaintRepository complaints,
            ParkingSlotRepository slots,
            ActivityLogRepository activity,
            java.time.Clock clock) {
        this.guard = guard;
        this.carts = carts;
        this.wishlists = wishlists;
        this.notifications = notifications;
        this.orders = orders;
        this.orderMapper = orderMapper;
        this.shops = shops;
        this.products = products;
        this.inventories = inventories;
        this.settings = settings;
        this.payments = payments;
        this.users = users;
        this.owners = owners;
        this.complaints = complaints;
        this.slots = slots;
        this.activity = activity;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView customer(long userId) {
        var customer = guard.requireCustomer(userId);
        DashboardView view = new DashboardView();
        Cart cart = carts.findByCustomerIdAndStatus(customer.getId(), CartStatus.OPEN).orElse(null);
        view.getCounts().put("cartLines", Integer.toString(cart == null ? 0 : cart.getItems().size()));
        int wished = wishlists.findByCustomerId(customer.getId()).map(list -> list.getItems().size()).orElse(0);
        view.getCounts().put("wishlistItems", Integer.toString(wished));
        view.getCounts().put("unread", Long.toString(notifications.countByUserIdAndReadAtIsNull(userId)));
        view.setRecentOrders(orders.findTop5ByCustomerIdOrderByPlacedAtDesc(customer.getId()).stream().map(orderMapper::toView).toList());
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView owner(long userId) {
        List<Long> shopIds = shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
        DashboardView view = new DashboardView();
        view.getCounts().put("shops", Integer.toString(shopIds.size()));
        view.getCounts().put("activeProducts", Long.toString(shopIds.isEmpty() ? 0 : products.countByShopIdInAndStatus(shopIds, ProductStatus.ACTIVE)));
        int threshold = settings.findById(1L).orElseThrow().getLowStockThreshold();
        long low = shopIds.isEmpty() ? 0 : inventories.findForShops(shopIds).stream()
                .filter(inventory -> inventory.getQuantityOnHand() <= threshold)
                .count();
        view.getCounts().put("lowStock", Long.toString(low));
        for (OrderStatus status : OrderStatus.values()) {
            view.getCounts().put(status.name(), "0");
        }
        if (!shopIds.isEmpty()) {
            for (Object[] pair : orders.countByStatusForShops(shopIds)) {
                view.getCounts().put(((OrderStatus) pair[0]).name(), Long.toString((Long) pair[1]));
            }
        }
        view.getCounts().put("revenueToday", plain(todayRevenue(shopIds)));
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView admin() {
        DashboardView view = new DashboardView();
        for (RoleName role : RoleName.values()) {
            view.getCounts().put(role.name(), Long.toString(users.countByRole(role)));
        }
        view.getCounts().put("pendingOwners", Long.toString(owners.countByApprovalStatus(ApprovalStatus.PENDING)));
        LocalDateTime start = todayStart();
        LocalDateTime end = start.plusDays(1);
        view.getCounts().put("ordersToday", Long.toString(orders.countByPlacedAtGreaterThanEqualAndPlacedAtLessThan(start, end)));
        view.getCounts().put("revenueToday", plain(payments.sumPaid(PaymentStatus.SUCCESS, start, end)));
        view.getCounts().put("openComplaints", Long.toString(complaints.countByStatus(ComplaintStatus.OPEN)));
        view.getCounts().put("occupiedSlots", Long.toString(slots.countByStatus(ParkingSlotStatus.OCCUPIED)));
        view.getCounts().put("totalSlots", Long.toString(slots.count()));
        for (Object[] pair : orders.countByStatus()) {
            view.getCounts().put("status_" + ((OrderStatus) pair[0]).name(), Long.toString((Long) pair[1]));
        }
        for (ActivityLog log : activity.findTop10ByOrderByCreatedAtDesc()) {
            view.getActivity().add(log.getAction() + " — " + log.getSummary());
        }
        return view;
    }

    private BigDecimal todayRevenue(List<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Money.ZERO;
        }
        LocalDateTime start = todayStart();
        return payments.sumPaidForShops(PaymentStatus.SUCCESS, start, start.plusDays(1), shopIds);
    }

    private LocalDateTime todayStart() {
        LocalDate today = LocalDate.now(clock.withZone(TimeDisplay.ZONE));
        return today.atStartOfDay(TimeDisplay.ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static String plain(BigDecimal amount) {
        return Money.scale(amount).toPlainString();
    }
}
