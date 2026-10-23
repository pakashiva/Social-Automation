package com.shoppingmall.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MoneySplit {

    private MoneySplit() {
    }

    public record Line(long productId, BigDecimal subtotal) {
    }

    public static Map<Long, BigDecimal> split(BigDecimal discount, List<Line> lines) {
        if (discount == null) {
            throw new IllegalArgumentException("discount is required");
        }
        if (discount.signum() < 0 || discount.scale() > 2) {
            throw new IllegalArgumentException("discount must be non-negative with scale at most 2");
        }
        long discountPaise = paise(discount);
        long eligible = 0L;
        List<long[]> rows = new ArrayList<>();
        for (Line line : lines) {
            if (line.subtotal() == null || line.subtotal().signum() < 0 || line.subtotal().scale() > 2) {
                throw new IllegalArgumentException("line subtotal must be non-negative with scale at most 2");
            }
            long linePaise = paise(line.subtotal());
            eligible += linePaise;
            rows.add(new long[] {line.productId(), linePaise, 0L, 0L});
        }
        if (discountPaise > eligible) {
            throw new IllegalArgumentException("discount paise exceed the eligible paise");
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (discountPaise == 0 || eligible == 0) {
            for (Line line : lines) {
                result.put(line.productId(), new BigDecimal("0.00"));
            }
            return result;
        }
        long allocated = 0L;
        for (long[] row : rows) {
            long share = discountPaise * row[1] / eligible;
            long fraction = discountPaise * row[1] % eligible;
            row[2] = share;
            row[3] = fraction;
            allocated += share;
        }
        long leftover = discountPaise - allocated;
        rows.stream()
                .sorted(Comparator.comparingLong((long[] row) -> row[3]).reversed().thenComparingLong(row -> row[0]))
                .limit(leftover)
                .forEach(row -> row[2] = row[2] + 1);
        for (long[] row : rows) {
            result.put(row[0], BigDecimal.valueOf(row[2], 2));
        }
        return result;
    }

    public static BigDecimal percentDiscount(BigDecimal eligible, BigDecimal percent, BigDecimal maxDiscount) {
        BigDecimal raw = eligible.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (maxDiscount != null && raw.compareTo(maxDiscount) > 0) {
            raw = maxDiscount.setScale(2, RoundingMode.HALF_UP);
        }
        if (raw.compareTo(eligible) > 0) {
            raw = eligible.setScale(2, RoundingMode.HALF_UP);
        }
        if (raw.signum() < 0) {
            return new BigDecimal("0.00");
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal fixedDiscount(BigDecimal eligible, BigDecimal fixed) {
        BigDecimal value = fixed.min(eligible);
        if (value.signum() < 0) {
            return new BigDecimal("0.00");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal tax(BigDecimal net, BigDecimal ratePercent) {
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }
        return net.multiply(ratePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static String rupees(BigDecimal amount) {
        BigDecimal scaled = amount == null ? new BigDecimal("0.00") : amount.setScale(2, RoundingMode.HALF_UP);
        return "₹" + scaled.toPlainString();
    }

    private static long paise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }
}
