package com.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter @Setter
public class CancelOrderForm {
    @NotBlank @Size(min = 5, max = 500)
    private String cancelReason;
}
