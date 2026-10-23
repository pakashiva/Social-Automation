package com.shoppingmall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "app_settings")
@EntityListeners(AuditingEntityListener.class)
public class AppSetting {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "mall_name", nullable = false, length = 160)
    private String mallName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "default_tax_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultTaxPercent;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Column(name = "max_upload_bytes", nullable = false)
    private int maxUploadBytes;

    @Column(name = "support_email", nullable = false, length = 255)
    private String supportEmail;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "support_phone", nullable = false, length = 10)
    private String supportPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
