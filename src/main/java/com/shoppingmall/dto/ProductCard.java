package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class ProductCard {
    private Long id;
    private Long shopId;
    private Long categoryId;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private BigDecimal unitPrice;
    private BigDecimal compareAtPrice;
    private BigDecimal taxRatePercent;
    private String status;
    private String imageUrl;
    private String shopName;
    private String shopSlug;
    private String categoryName;
    private Integer stock;
    private String priceLabel;
}
