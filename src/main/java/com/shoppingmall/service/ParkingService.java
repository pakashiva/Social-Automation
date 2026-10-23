package com.shoppingmall.service;

import com.shoppingmall.dto.ParkingEntryForm;
import com.shoppingmall.dto.ParkingRateForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.enums.ParkingSlotStatus;
import com.shoppingmall.enums.VehicleType;

public interface ParkingService {

    Rows.ParkingBoard board();

    void enter(long adminId, ParkingEntryForm form);

    void exit(String ticketNumber);

    void voidOpen(long adminId, String ticketNumber);

    void updateRate(long adminId, VehicleType vehicleType, ParkingRateForm form);

    void changeSlot(long slotId, ParkingSlotStatus status);
}
