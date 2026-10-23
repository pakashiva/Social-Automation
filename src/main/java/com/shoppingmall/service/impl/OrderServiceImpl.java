package com.shoppingmall.service.impl;

import com.shoppingmall.dto.OrderView;
import com.shoppingmall.entity.Inventory;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.entity.OrderStatusHistory;
import com.shoppingmall.entity.Payment;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.enums.StockMovementType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.InventoryRepository;
import com.shoppingmall.repository.MallOrderRepository;
import com.shoppingmall.repository.OrderStatusHistoryRepository;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.OrderService;
import com.shoppingmall.util.Pages;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final MallOrderRepository orders;
    private final PaymentRepository payments;
    private final InventoryRepository inventories;
    private final OrderStatusHistoryRepository history;
    private final ShopRepository shops;
    private final OwnershipGuard guard;
    private final StockLedger ledger;
    private final NotificationService notifications;
    private final AuditService audit;
    private final OrderMapper mapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public OrderServiceImpl(
            MallOrderRepository orders,
            PaymentRepository payments,
            InventoryRepository inventories,
            OrderStatusHistoryRepository history,
            ShopRepository shops,
            OwnershipGuard guard,
            StockLedger ledger,
            NotificationService notifications,
            AuditService audit,
            OrderMapper mapper,
            Clock clock,
            EntityManager entityManager) {
        this.orders = orders;
        this.payments = payments;
        this.inventories = inventories;
        this.history = history;
        this.shops = shops;
        this.guard = guard;
        this.ledger = ledger;
        this.notifications = notifications;
        this.audit = audit;
        this.mapper = mapper;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderView> listForCustomer(long userId, int page) {
        return orders.findByCustomerIdOrderByPlacedAtDesc(guard.requireCustomer(userId).getId(), Pages.of(page))
                .map(mapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getForCustomer(long userId, String orderNumber) {
        return mapper.toView(guard.requireOwnCustomerOrder(userId, orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderView> listForOwner(long userId, int page) {
        List<Long> shopIds = shops.findByOwnerId(guard.requireOwner(userId).getId()).stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return Page.empty(Pages.of(page));
        }
        return orders.findByShopIdInOrderByPlacedAtDesc(shopIds, Pages.of(page)).map(mapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getForOwner(long userId, String orderNumber) {
        return mapper.toView(guard.requireOwnOrder(userId, orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderView> listAll(int page) {
        return orders.findAllByOrderByPlacedAtDesc(Pages.of(page)).map(mapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView get(String orderNumber) {
        return mapper.toView(orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order was not found.")));
    }

    @Override
    @Transactional
    public void changeStatus(long userId, String orderNumber, OrderStatus status) {
        MallOrder order = guard.isAdmin(userId)
                ? orders.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order was not found."))
                : guard.requireOwnOrder(userId, orderNumber);
        OrderStatus allowed = switch (order.getStatus()) {
            case CONFIRMED -> OrderStatus.PACKED;
            case PACKED -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            default -> null;
        };
        if (allowed == null || status != allowed) {
            throw new BusinessException("This order cannot move to that status.", "/owner/orders/" + orderNumber);
        }
        move(order, status, guard.requireUser(userId).getId(), null);
    }

    @Override
    @Transactional
    public void cancelByCustomer(long userId, String orderNumber, String reason) {
        MallOrder order = guard.requireOwnCustomerOrder(userId, orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("This order cannot be cancelled.", "/customer/orders/" + orderNumber);
        }
        cancel(order, reason, userId, false, "/customer/orders/" + orderNumber);
    }

    @Override
    @Transactional
    public void cancelByOwner(long userId, String orderNumber, String reason) {
        MallOrder order = guard.requireOwnOrder(userId, orderNumber);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("This order cannot be cancelled.", "/owner/orders/" + orderNumber);
        }
        cancel(order, reason, userId, false, "/owner/orders/" + orderNumber);
    }

    @Override
    @Transactional
    public void cancelByAdmin(long adminId, String orderNumber, String reason) {
        MallOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order was not found."));
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PACKED) {
            throw new BusinessException("This order cannot be cancelled.", "/admin/orders/" + orderNumber);
        }
        cancel(order, reason, adminId, true, "/admin/orders/" + orderNumber);
    }

    private void cancel(MallOrder order, String reason, long actorId, boolean admin, String redirect) {
        String text = reason == null ? "" : reason.trim();
        if (text.length() < 5 || text.length() > 500) {
            throw new BusinessException("Enter a reason between 5 and 500 characters.", redirect);
        }
        Payment payment = payments.findByOrderId(order.getId()).orElseThrow();
        entityManager.refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        List<OrderItem> lines = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        boolean pending = order.getStatus() == OrderStatus.PENDING_PAYMENT;
        for (OrderItem line : lines) {
            Inventory inventory = inventories.findByProductId(line.getProduct().getId()).orElseThrow();
            entityManager.refresh(inventory, LockModeType.PESSIMISTIC_WRITE);
            if (pending) {
                ledger.apply(inventory, line.getQuantity(), -line.getQuantity(), StockMovementType.RELEASE, "ORDER",
                        order.getId(), "Cancelled before payment", guard.requireUser(actorId));
            } else {
                ledger.apply(inventory, line.getQuantity(), 0, StockMovementType.RETURN, "ORDER",
                        order.getId(), text, guard.requireUser(actorId));
            }
        }
        if (pending) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Cancelled before payment");
        } else if (payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now(clock));
        }
        order.setCancelReason(text);
        order.setCancelledAt(LocalDateTime.now(clock));
        move(order, OrderStatus.CANCELLED, actorId, text);
        if (admin) {
            audit.record(actorId, "ORDER_CANCELLED", "orders", order.getId(), "Cancelled " + order.getOrderNumber() + ".");
            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                audit.record(actorId, "PAYMENT_REFUNDED", "payments", payment.getId(),
                        "Refunded " + payment.getPaymentNumber() + ".");
            }
        }
    }

    private void move(MallOrder order, OrderStatus status, long actorId, String note) {
        OrderStatus from = order.getStatus();
        order.setStatus(status);
        OrderStatusHistory row = new OrderStatusHistory();
        row.setOrder(order);
        row.setFromStatus(from);
        row.setToStatus(status);
        row.setChangedBy(guard.requireUser(actorId));
        row.setNote(note);
        row.setCreatedAt(LocalDateTime.now(clock));
        history.save(row);
        notifications.notifyUser(order.getCustomer().getUser().getId(), NotificationType.ORDER_STATUS,
                "Order " + order.getOrderNumber(),
                "Order " + order.getOrderNumber() + " is now " + status + ".",
                "/customer/orders/" + order.getOrderNumber());
    }
}
