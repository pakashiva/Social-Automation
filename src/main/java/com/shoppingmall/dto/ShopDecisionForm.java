package com.shoppingmall.dto;


import com.shoppingmall.enums.ShopStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ShopDecisionForm {
    @Size(max = 500)
    private String rejectionReason;
    private ShopStatus status;
}
