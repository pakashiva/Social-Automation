package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Getter @Setter
public class SettingsForm {
    @NotBlank @Size(max = 160)
    private String mallName;
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal defaultTaxPercent;
    @NotNull @Min(0)
    private int lowStockThreshold;
    @NotNull @Min(1) @Max(2097152)
    private Integer maxUploadBytes;
    @NotBlank @Email
    private String supportEmail;
    @NotBlank @Pattern(regexp = "^[0-9]{10}$")
    private String supportPhone;
}
