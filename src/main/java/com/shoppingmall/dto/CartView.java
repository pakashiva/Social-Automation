package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class CartView {
    private List<Line> lines = new ArrayList<>();
    private BigDecimal subtotal = new BigDecimal("0.00");
    private BigDecimal discount = new BigDecimal("0.00");
    private BigDecimal tax = new BigDecimal("0.00");
    private BigDecimal total = new BigDecimal("0.00");
    private String couponCode;
    private boolean addressComplete;

    @Getter @Setter
    public static class Line {
        private Long id;
        private Long productId;
        private String productName;
        private String shopName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
