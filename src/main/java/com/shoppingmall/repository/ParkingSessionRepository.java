package com.shoppingmall.repository;

import com.shoppingmall.entity.ParkingSession;
import com.shoppingmall.enums.ParkingSessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findByTicketNumber(String ticketNumber);
    boolean existsByVehicleNumberAndStatus(String vehicleNumber, ParkingSessionStatus status);
    boolean existsBySlotIdAndStatus(Long slotId, ParkingSessionStatus status);
    List<ParkingSession> findByStatusOrderByEntryAtDesc(ParkingSessionStatus status);
}
