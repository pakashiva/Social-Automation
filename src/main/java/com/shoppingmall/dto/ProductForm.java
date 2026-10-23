package com.shoppingmall.dto;


import com.shoppingmall.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
public class ProductForm {
    @NotNull
    private Long shopId;
    @NotNull
    private Long categoryId;
    @NotBlank @Size(max = 180)
    private String name;
    @NotBlank @Size(max = 64)
    private String sku;
    @Size(max = 4000)
    private String description;
    @NotNull @DecimalMin("0.00")
    private BigDecimal unitPrice;
    @DecimalMin("0.00")
    private BigDecimal compareAtPrice;
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal taxRatePercent;
    @NotNull
    private ProductStatus status;
    @Min(value = 0, message = "Opening quantity cannot be negative.")
    private Integer openingQuantity;
    private MultipartFile image;
}
