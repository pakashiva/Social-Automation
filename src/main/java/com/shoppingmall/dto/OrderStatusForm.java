package com.shoppingmall.dto;


import com.shoppingmall.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderStatusForm {
    @NotNull
    private OrderStatus status;
}
