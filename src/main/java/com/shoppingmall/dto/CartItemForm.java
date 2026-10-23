package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter @Setter
public class CartItemForm {
    @NotNull
    private Long productId;
    @NotNull @Min(1) @Max(20)
    private Integer quantity;
}
