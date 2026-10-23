package com.shoppingmall.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.util.ParkingChargeCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ParkingChargeTest {

    private static final BigDecimal HOURLY = new BigDecimal("40.00");
    private static final BigDecimal DAILY = new BigDecimal("200.00");
    private final LocalDateTime entry = LocalDateTime.of(2026, 10, 23, 10, 0);

    @Test
    void reproducesTheFourCarExamples() {
        assertEquals(new BigDecimal("0.00"), charge(entry.plusMinutes(10)));
        assertEquals(new BigDecimal("40.00"), charge(entry.plusMinutes(20)));
        assertEquals(new BigDecimal("200.00"), charge(entry.plusHours(6).plusMinutes(30)));
        assertEquals(new BigDecimal("240.00"), charge(entry.plusMinutes(15 + 1500)));
    }

    @Test
    void exitBeforeEntryIsRejected() {
        BusinessException exception = assertThrows(BusinessException.class, () -> charge(entry.minusMinutes(1)));
        assertEquals("Exit time is before entry time.", exception.getMessage());
    }

    private BigDecimal charge(LocalDateTime exit) {
        return ParkingChargeCalculator.charge(15, HOURLY, DAILY, entry, exit);
    }
}
