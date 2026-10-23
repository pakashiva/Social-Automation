package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class CheckoutForm {
    @NotBlank
    private String checkoutToken;
    @Size(max = 40)
    private String couponCode;
    @Size(max = 500)
    private String customerNote;
}
