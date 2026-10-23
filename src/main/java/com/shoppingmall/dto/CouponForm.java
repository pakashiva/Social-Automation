package com.shoppingmall.dto;


import com.shoppingmall.enums.CouponScope;
import com.shoppingmall.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CouponForm {
    @NotBlank @Size(max = 40)
    private String code;
    @NotBlank @Size(max = 255)
    private String description;
    @NotNull
    private DiscountType discountType;
    @NotNull @DecimalMin(value = "0.01", message = "Discount must be greater than zero.")
    private BigDecimal discountValue;
    @NotNull @DecimalMin("0.00")
    private BigDecimal minOrderAmount;
    @DecimalMin("0.00")
    private BigDecimal maxDiscountAmount;
    @Min(1)
    private Integer usageLimit;
    @NotNull @Min(1)
    private Integer perCustomerLimit;
    @NotNull
    private LocalDateTime startsAt;
    @NotNull
    private LocalDateTime endsAt;
    @NotNull
    private CouponScope scope;
    private Long shopId;
    private Long categoryId;
}
