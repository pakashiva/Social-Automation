package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class OrderView {
    private String orderNumber;
    private String shopName;
    private Long shopId;
    private String customerName;
    private String status;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String couponCode;
    private String customerNote;
    private String cancelReason;
    private LocalDateTime placedAt;
    private boolean payAllowed;
    private boolean customerCancelAllowed;
    private boolean ownerCancelAllowed;
    private boolean adminCancelAllowed;
    private List<String> nextStatuses = new ArrayList<>();
    private List<Line> lines = new ArrayList<>();

    @Getter @Setter
    public static class Line {
        private Long orderItemId;
        private String productName;
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal lineTotal;
        private boolean reviewAllowed;
    }
}
