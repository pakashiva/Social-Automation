package com.shoppingmall.service.impl;

import com.shoppingmall.dto.OrderView;
import com.shoppingmall.entity.MallOrder;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.entity.Payment;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.PaymentStatus;
import com.shoppingmall.repository.PaymentRepository;
import com.shoppingmall.repository.ReviewRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final PaymentRepository payments;
    private final ReviewRepository reviews;

    public OrderMapper(PaymentRepository payments, ReviewRepository reviews) {
        this.payments = payments;
        this.reviews = reviews;
    }

    public OrderView toView(MallOrder order) {
        OrderView view = new OrderView();
        view.setOrderNumber(order.getOrderNumber());
        view.setShopName(order.getShop().getName());
        view.setShopId(order.getShop().getId());
        view.setCustomerName(order.getCustomer().getUser().getFullName());
        view.setStatus(order.getStatus().name());
        Payment payment = payments.findFirstByOrderId(order.getId()).orElse(null);
        view.setPaymentStatus(payment == null ? "" : payment.getStatus().name());
        view.setSubtotal(order.getSubtotal());
        view.setDiscount(order.getDiscountAmount());
        view.setTax(order.getTaxAmount());
        view.setTotal(order.getTotalAmount());
        view.setCouponCode(order.getCouponCodeSnapshot());
        view.setCustomerNote(order.getCustomerNote());
        view.setCancelReason(order.getCancelReason());
        view.setPlacedAt(order.getPlacedAt());
        view.setPayAllowed(order.getStatus() == OrderStatus.PENDING_PAYMENT
                && payment != null && payment.getStatus() == PaymentStatus.PENDING);
        view.setCustomerCancelAllowed(order.getStatus() == OrderStatus.PENDING_PAYMENT
                || order.getStatus() == OrderStatus.CONFIRMED);
        view.setOwnerCancelAllowed(order.getStatus() == OrderStatus.CONFIRMED);
        view.setAdminCancelAllowed(order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PACKED);
        view.setNextStatuses(next(order.getStatus()));
        for (OrderItem item : order.getItems()) {
            OrderView.Line line = new OrderView.Line();
            line.setOrderItemId(item.getId());
            line.setProductName(item.getProductNameSnapshot());
            line.setSku(item.getSkuSnapshot());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            line.setDiscount(item.getDiscountAmount());
            line.setTax(item.getTaxAmount());
            line.setLineTotal(item.getLineTotal());
            line.setReviewAllowed(order.getStatus() == OrderStatus.DELIVERED && !reviews.existsByOrderItemId(item.getId()));
            view.getLines().add(line);
        }
        return view;
    }

    private static List<String> next(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> List.of(OrderStatus.PACKED.name());
            case PACKED -> List.of(OrderStatus.SHIPPED.name());
            case SHIPPED -> List.of(OrderStatus.DELIVERED.name());
            default -> List.of();
        };
    }
}
