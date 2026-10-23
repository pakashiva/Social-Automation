package com.shoppingmall.dto;


import com.shoppingmall.enums.ParkingSlotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SlotStatusForm {
    @NotNull
    private ParkingSlotStatus status;
}
