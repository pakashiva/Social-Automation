package com.shoppingmall.util;

import com.shoppingmall.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class ParkingChargeCalculator {

    private ParkingChargeCalculator() {
    }

    public static BigDecimal charge(
            int graceMinutes,
            BigDecimal hourlyRate,
            BigDecimal dailyCap,
            LocalDateTime entry,
            LocalDateTime exit) {
        if (exit.isBefore(entry)) {
            throw new BusinessException("Exit time is before entry time.");
        }
        long durationMinutes = Duration.between(entry, exit).toMinutes();
        long billable = Math.max(0, durationMinutes - graceMinutes);
        if (billable == 0) {
            return new BigDecimal("0.00");
        }
        long fullDays = billable / 1440;
        long remainder = billable % 1440;
        long remainderHours = remainder == 0 ? 0 : (remainder + 59) / 60;
        BigDecimal remainderCharge = hourlyRate.multiply(BigDecimal.valueOf(remainderHours));
        if (remainderCharge.compareTo(dailyCap) > 0) {
            remainderCharge = dailyCap;
        }
        BigDecimal total = dailyCap.multiply(BigDecimal.valueOf(fullDays)).add(remainderCharge);
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
