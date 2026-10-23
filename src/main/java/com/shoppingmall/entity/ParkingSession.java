package com.shoppingmall.entity;

import com.shoppingmall.enums.ParkingSessionStatus;
import com.shoppingmall.enums.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parking_sessions")
public class ParkingSession extends AuditedEntity {

    @Column(name = "ticket_number", nullable = false, length = 32)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ParkingSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @Column(name = "entry_at", nullable = false)
    private LocalDateTime entryAt;

    @Column(name = "exit_at")
    private LocalDateTime exitAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_id", nullable = false)
    private ParkingRate rate;

    @Column(name = "grace_minutes_snapshot", nullable = false)
    private int graceMinutesSnapshot;

    @Column(name = "hourly_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRateSnapshot;

    @Column(name = "daily_cap_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCapSnapshot;

    @Column(name = "charged_amount", precision = 10, scale = 2)
    private BigDecimal chargedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParkingSessionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;
}
