package com.shoppingmall.dto;


import com.shoppingmall.enums.AdjustmentDirection;
import com.shoppingmall.enums.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StockAdjustForm {
    @NotNull
    private StockMovementType movementType;
    private AdjustmentDirection direction;
    @NotNull @Min(value = 1, message = "Quantity must be a positive whole number.")
    private Integer quantity;
    @NotBlank @Size(min = 5, max = 500)
    private String note;
}
