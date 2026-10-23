package com.shoppingmall.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.dto.OrderView;
import com.shoppingmall.enums.CouponRejectReason;
import com.shoppingmall.enums.CouponScope;
import com.shoppingmall.enums.DiscountType;
import com.shoppingmall.exception.CouponRejectedException;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CouponRulesTest extends MallSpringTest {

    @Autowired
    private CheckoutService checkout;

    @Test
    void expiredLimitMinimumAndEligibilityAreRejected() {
        TestData.Store store = data.store();
        TestData.Store other = data.store();
        long productId = data.product(store, new BigDecimal("40.00"), new BigDecimal("0.00"), 20);
        LocalDateTime now = LocalDateTime.now();

        assertReason(productId, data.coupon(store.userId(), CouponScope.MALL, DiscountType.PERCENT,
                new BigDecimal("10.00"), new BigDecimal("0.00"), 10, 0, null, null,
                now.minusDays(10), now.minusDays(1)), CouponRejectReason.EXPIRED);

        assertReason(productId, data.coupon(store.userId(), CouponScope.MALL, DiscountType.PERCENT,
                new BigDecimal("10.00"), new BigDecimal("0.00"), 1, 1, null, null,
                now.minusDays(1), now.plusDays(1)), CouponRejectReason.USAGE_LIMIT);

        assertReason(productId, data.coupon(store.userId(), CouponScope.MALL, DiscountType.PERCENT,
                new BigDecimal("10.00"), new BigDecimal("500.00"), 10, 0, null, null,
                now.minusDays(1), now.plusDays(1)), CouponRejectReason.MINIMUM);

        assertReason(productId, data.coupon(store.userId(), CouponScope.SHOP, DiscountType.FIXED,
                new BigDecimal("10.00"), new BigDecimal("0.00"), 10, 0, other.shopId(), null,
                now.minusDays(1), now.plusDays(1)), CouponRejectReason.NOT_ELIGIBLE);
    }

    @Test
    void fixedDiscountStopsAtZero() {
        TestData.Store store = data.store();
        long productId = data.product(store, new BigDecimal("40.00"), new BigDecimal("0.00"), 20);
        TestData.Person customer = data.customer();
        data.addToCart(customer.userId(), productId, 1);
        LocalDateTime now = LocalDateTime.now();
        String code = data.coupon(store.userId(), CouponScope.MALL, DiscountType.FIXED,
                new BigDecimal("50.00"), new BigDecimal("0.00"), 10, 0, null, null,
                now.minusDays(1), now.plusDays(1));
        OrderView order = checkout.place(customer.userId(), UUID.randomUUID().toString(), code, null).get(0);
        assertEquals(0, order.getSubtotal().compareTo(new BigDecimal("40.00")));
        assertEquals(0, order.getDiscount().compareTo(new BigDecimal("40.00")));
        assertEquals(0, order.getTax().compareTo(new BigDecimal("0.00")));
        assertEquals(0, order.getTotal().compareTo(new BigDecimal("0.00")));
    }

    private void assertReason(long productId, String code, CouponRejectReason reason) {
        TestData.Person customer = data.customer();
        data.addToCart(customer.userId(), productId, 1);
        CouponRejectedException exception = assertThrows(CouponRejectedException.class,
                () -> checkout.place(customer.userId(), UUID.randomUUID().toString(), code, null));
        assertEquals(reason, exception.getReason());
    }
}
