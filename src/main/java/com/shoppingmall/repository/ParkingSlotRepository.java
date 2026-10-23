package com.shoppingmall.repository;

import com.shoppingmall.entity.ParkingSlot;
import com.shoppingmall.enums.ParkingSlotStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSlot s where s.id = :id")
    Optional<ParkingSlot> lockById(@Param("id") Long id);

    long countByStatus(ParkingSlotStatus status);
}
