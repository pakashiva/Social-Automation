package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class InvoiceView {
    private String invoiceNumber;
    private String orderNumber;
    private String shopName;
    private String billedName;
    private String billedEmail;
    private String billedMobile;
    private String billedAddress;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private LocalDateTime issuedAt;
    private List<OrderView.Line> lines = new ArrayList<>();
}
