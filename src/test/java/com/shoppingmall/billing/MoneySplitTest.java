package com.shoppingmall.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.util.MoneySplit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MoneySplitTest {

    @Test
    void percentDiscountRespectsTheCap() {
        BigDecimal discount = MoneySplit.percentDiscount(new BigDecimal("1000.00"), new BigDecimal("10"), new BigDecimal("300.00"));
        assertEquals(new BigDecimal("100.00"), discount);
        assertEquals(new BigDecimal("45.00"), MoneySplit.tax(new BigDecimal("900.00"), new BigDecimal("5.00")));
    }

    @Test
    void fixedDiscountCannotExceedTheEligibleSubtotal() {
        assertEquals(new BigDecimal("40.00"), MoneySplit.fixedDiscount(new BigDecimal("40.00"), new BigDecimal("50.00")));
        assertEquals(new BigDecimal("0.00"), MoneySplit.tax(new BigDecimal("0.00"), new BigDecimal("5.00")));
    }

    @Test
    void leftoverPaiseGoesToTheSmallerProductIdOnATie() {
        Map<Long, BigDecimal> shares = MoneySplit.split(new BigDecimal("0.01"), List.of(
                new MoneySplit.Line(2L, new BigDecimal("1.00")),
                new MoneySplit.Line(1L, new BigDecimal("1.00"))));
        assertEquals(new BigDecimal("0.01"), shares.get(1L));
        assertEquals(new BigDecimal("0.00"), shares.get(2L));
    }

    @Test
    void rejectsNegativeScaleAndOversizeDiscounts() {
        assertThrows(IllegalArgumentException.class,
                () -> MoneySplit.split(new BigDecimal("-1.00"), List.of(new MoneySplit.Line(1L, new BigDecimal("1.00")))));
        assertThrows(IllegalArgumentException.class,
                () -> MoneySplit.split(new BigDecimal("1.001"), List.of(new MoneySplit.Line(1L, new BigDecimal("2.00")))));
        assertThrows(IllegalArgumentException.class,
                () -> MoneySplit.split(new BigDecimal("3.00"), List.of(new MoneySplit.Line(1L, new BigDecimal("2.00")))));
    }
}
