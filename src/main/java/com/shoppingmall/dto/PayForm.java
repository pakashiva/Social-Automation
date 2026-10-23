package com.shoppingmall.dto;


import com.shoppingmall.enums.SimulatedOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PayForm {
    @NotNull
    private SimulatedOutcome outcome;
}
