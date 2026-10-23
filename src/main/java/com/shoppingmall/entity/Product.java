package com.shoppingmall.entity;

import com.shoppingmall.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "slug", nullable = false, length = 200)
    private String slug;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "compare_at_price", precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRatePercent;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private Inventory inventory;
}
