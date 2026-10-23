package com.shoppingmall.dto;


import com.shoppingmall.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ParkingEntryForm {
    @NotNull
    private Long slotId;
    @NotBlank
    private String vehicleNumber;
    @NotNull
    private VehicleType vehicleType;
}
