package com.shoppingmall.repository;

import com.shoppingmall.entity.ParkingRate;
import com.shoppingmall.enums.VehicleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingRateRepository extends JpaRepository<ParkingRate, Long> {
    Optional<ParkingRate> findByVehicleType(VehicleType vehicleType);
}
