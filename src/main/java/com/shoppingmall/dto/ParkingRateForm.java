package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter @Setter
public class ParkingRateForm {
    @NotNull @Min(0)
    private Integer graceMinutes;
    @NotNull @DecimalMin("0.00")
    private BigDecimal hourlyRate;
    @NotNull @DecimalMin("0.00")
    private BigDecimal dailyCap;
}
