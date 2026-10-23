package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ParkingEntryForm;
import com.shoppingmall.dto.ParkingRateForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.ParkingRate;
import com.shoppingmall.entity.ParkingSession;
import com.shoppingmall.entity.ParkingSlot;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.enums.ParkingSessionStatus;
import com.shoppingmall.enums.ParkingSlotStatus;
import com.shoppingmall.enums.VehicleType;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.ParkingRateRepository;
import com.shoppingmall.repository.ParkingSessionRepository;
import com.shoppingmall.repository.ParkingSlotRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.NumberSequenceService;
import com.shoppingmall.service.ParkingService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.ParkingChargeCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingServiceImpl implements ParkingService {

    private static final String PLATE = "^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$";

    private final ParkingSlotRepository slots;
    private final ParkingSessionRepository sessions;
    private final ParkingRateRepository rates;
    private final OwnershipGuard guard;
    private final NumberSequenceService sequences;
    private final AuditService audit;
    private final NotificationService notifications;
    private final Clock clock;

    public ParkingServiceImpl(
            ParkingSlotRepository slots,
            ParkingSessionRepository sessions,
            ParkingRateRepository rates,
            OwnershipGuard guard,
            NumberSequenceService sequences,
            AuditService audit,
            NotificationService notifications,
            Clock clock) {
        this.slots = slots;
        this.sessions = sessions;
        this.rates = rates;
        this.guard = guard;
        this.sequences = sequences;
        this.audit = audit;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.ParkingBoard board() {
        Rows.ParkingBoard board = new Rows.ParkingBoard();
        slots.findAll().stream()
                .sorted(Comparator.comparing(ParkingSlot::getSlotCode))
                .forEach(slot -> board.getSlots().add(slotRow(slot)));
        sessions.findByStatusOrderByEntryAtDesc(ParkingSessionStatus.OPEN)
                .forEach(session -> board.getOpenSessions().add(sessionRow(session)));
        rates.findAll().stream()
                .sorted(Comparator.comparing(rate -> rate.getVehicleType().name()))
                .forEach(rate -> board.getRates().add(rateRow(rate)));
        return board;
    }

    @Override
    @Transactional
    public void enter(long adminId, ParkingEntryForm form) {
        ParkingSlot slot = slots.lockById(form.getSlotId())
                .orElseThrow(() -> new NotFoundException("Slot was not found."));
        if (slot.getStatus() != ParkingSlotStatus.AVAILABLE) {
            throw new BusinessException("That slot is not available.", "/admin/parking");
        }
        if (form.getVehicleType() == null || slot.getVehicleType() != form.getVehicleType()) {
            throw new BusinessException("Choose a slot for that vehicle type.", "/admin/parking");
        }
        String plate = form.getVehicleNumber() == null ? "" : form.getVehicleNumber().trim().toUpperCase();
        if (!plate.matches(PLATE)) {
            throw new BusinessException("Enter a vehicle number like KA01AB1234.", "/admin/parking");
        }
        if (sessions.existsByVehicleNumberAndStatus(plate, ParkingSessionStatus.OPEN)) {
            throw new BusinessException("That vehicle is already parked.", "/admin/parking");
        }
        ParkingRate rate = rates.findByVehicleType(form.getVehicleType())
                .filter(ParkingRate::isActive)
                .orElseThrow(() -> new BusinessException("That vehicle type has no rate.", "/admin/parking"));
        ParkingSession session = new ParkingSession();
        session.setTicketNumber(sequences.next("PARKING"));
        session.setSlot(slot);
        session.setVehicleType(form.getVehicleType());
        session.setVehicleNumber(plate);
        session.setEntryAt(LocalDateTime.now(clock));
        session.setRate(rate);
        session.setGraceMinutesSnapshot(rate.getGraceMinutes());
        session.setHourlyRateSnapshot(Money.scale(rate.getHourlyRate()));
        session.setDailyCapSnapshot(Money.scale(rate.getDailyCap()));
        session.setStatus(ParkingSessionStatus.OPEN);
        session.setRecordedBy(guard.requireUser(adminId));
        sessions.save(session);
        slot.setStatus(ParkingSlotStatus.OCCUPIED);
        slots.save(slot);
    }

    @Override
    @Transactional
    public void exit(String ticketNumber) {
        ParkingSession session = open(ticketNumber);
        LocalDateTime exitAt = LocalDateTime.now(clock);
        BigDecimal charge = ParkingChargeCalculator.charge(
                session.getGraceMinutesSnapshot(),
                session.getHourlyRateSnapshot(),
                session.getDailyCapSnapshot(),
                session.getEntryAt(),
                exitAt);
        session.setExitAt(exitAt);
        session.setChargedAmount(charge);
        session.setStatus(ParkingSessionStatus.CLOSED);
        sessions.save(session);
        release(session.getSlot());
        notifications.notifyUser(
                session.getRecordedBy().getId(),
                NotificationType.PARKING,
                "Parking closed",
                session.getTicketNumber() + " charged " + charge.toPlainString(),
                "/admin/parking");
    }

    @Override
    @Transactional
    public void voidOpen(long adminId, String ticketNumber) {
        ParkingSession session = open(ticketNumber);
        session.setExitAt(LocalDateTime.now(clock));
        session.setChargedAmount(new BigDecimal("0.00"));
        session.setStatus(ParkingSessionStatus.VOID);
        sessions.save(session);
        release(session.getSlot());
        audit.record(adminId, "PARKING_VOIDED", "parking_sessions", session.getId(), session.getTicketNumber());
    }

    @Override
    @Transactional
    public void updateRate(long adminId, VehicleType vehicleType, ParkingRateForm form) {
        if (form.getGraceMinutes() == null || form.getGraceMinutes() < 0
                || form.getHourlyRate() == null || form.getHourlyRate().signum() < 0
                || form.getDailyCap() == null || form.getDailyCap().compareTo(form.getHourlyRate()) < 0) {
            throw new BusinessException("Daily cap must be at least the hourly rate.", "/admin/parking");
        }
        ParkingRate rate = rates.findByVehicleType(vehicleType)
                .orElseThrow(() -> new NotFoundException("Rate was not found."));
        rate.setGraceMinutes(form.getGraceMinutes());
        rate.setHourlyRate(Money.scale(form.getHourlyRate()));
        rate.setDailyCap(Money.scale(form.getDailyCap()));
        rates.save(rate);
        audit.record(adminId, "PARKING_RATE_UPDATED", "parking_rates", rate.getId(), vehicleType.name());
    }

    @Override
    @Transactional
    public void changeSlot(long slotId, ParkingSlotStatus status) {
        if (status != ParkingSlotStatus.AVAILABLE && status != ParkingSlotStatus.OUT_OF_SERVICE) {
            throw new BusinessException("Choose available or out of service.", "/admin/parking");
        }
        ParkingSlot slot = slots.lockById(slotId).orElseThrow(() -> new NotFoundException("Slot was not found."));
        if (sessions.existsBySlotIdAndStatus(slot.getId(), ParkingSessionStatus.OPEN)) {
            throw new BusinessException("That slot is not available.", "/admin/parking");
        }
        slot.setStatus(status);
        slots.save(slot);
    }

    private ParkingSession open(String ticketNumber) {
        ParkingSession session = sessions.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new NotFoundException("Ticket was not found."));
        if (session.getStatus() != ParkingSessionStatus.OPEN) {
            throw new BusinessException("That ticket is already closed.", "/admin/parking");
        }
        return session;
    }

    private void release(ParkingSlot slot) {
        ParkingSlot locked = slots.lockById(slot.getId()).orElseThrow();
        if (locked.getStatus() != ParkingSlotStatus.OUT_OF_SERVICE) {
            locked.setStatus(ParkingSlotStatus.AVAILABLE);
            slots.save(locked);
        }
    }

    private static Rows.ParkingSlotRow slotRow(ParkingSlot slot) {
        Rows.ParkingSlotRow row = new Rows.ParkingSlotRow();
        row.setId(slot.getId());
        row.setSlotCode(slot.getSlotCode());
        row.setZone(slot.getZone());
        row.setVehicleType(slot.getVehicleType().name());
        row.setStatus(slot.getStatus().name());
        return row;
    }

    private static Rows.ParkingSessionRow sessionRow(ParkingSession session) {
        Rows.ParkingSessionRow row = new Rows.ParkingSessionRow();
        row.setTicketNumber(session.getTicketNumber());
        row.setSlotCode(session.getSlot().getSlotCode());
        row.setVehicleNumber(session.getVehicleNumber());
        row.setVehicleType(session.getVehicleType().name());
        row.setEntryAt(session.getEntryAt());
        row.setExitAt(session.getExitAt());
        row.setChargedAmount(session.getChargedAmount());
        row.setStatus(session.getStatus().name());
        return row;
    }

    private static Rows.ParkingRateRow rateRow(ParkingRate rate) {
        Rows.ParkingRateRow row = new Rows.ParkingRateRow();
        row.setVehicleType(rate.getVehicleType().name());
        row.setGraceMinutes(rate.getGraceMinutes());
        row.setHourlyRate(rate.getHourlyRate());
        row.setDailyCap(rate.getDailyCap());
        return row;
    }
}
