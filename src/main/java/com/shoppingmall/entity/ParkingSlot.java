package com.shoppingmall.entity;

import com.shoppingmall.enums.ParkingSlotStatus;
import com.shoppingmall.enums.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parking_slots")
public class ParkingSlot extends AuditedEntity {

    @Column(name = "slot_code", nullable = false, length = 20)
    private String slotCode;

    @Column(name = "zone", nullable = false, length = 40)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParkingSlotStatus status;
}
