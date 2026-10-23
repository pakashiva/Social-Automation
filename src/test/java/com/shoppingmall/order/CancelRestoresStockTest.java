package com.shoppingmall.order;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shoppingmall.enums.SimulatedOutcome;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.service.OrderService;
import com.shoppingmall.service.PaymentService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelRestoresStockTest extends MallSpringTest {

    @Autowired
    private CheckoutService checkout;

    @Autowired
    private PaymentService payments;

    @Autowired
    private OrderService orders;

    @Test
    void cancellingAPendingOrderReleasesTheReservation() {
        long productId = stockedProduct();
        TestData.Person customer = data.customer();
        String orderNumber = place(customer.userId(), productId);
        assertArrayEquals(new int[] {9, 1}, data.stock(productId));

        orders.cancelByCustomer(customer.userId(), orderNumber, "Changed my mind");

        assertArrayEquals(new int[] {10, 0}, data.stock(productId));
        assertEquals("FAILED", data.paymentStatus(orderNumber));
        assertEquals("Cancelled before payment", data.failureReason(orderNumber));
    }

    @Test
    void cancellingAConfirmedOrderRefundsAndRestoresOnHand() {
        long productId = stockedProduct();
        TestData.Person customer = data.customer();
        String orderNumber = place(customer.userId(), productId);
        payments.pay(customer.userId(), orderNumber, SimulatedOutcome.SUCCESS);
        assertArrayEquals(new int[] {9, 0}, data.stock(productId));

        orders.cancelByCustomer(customer.userId(), orderNumber, "Changed my mind");

        assertArrayEquals(new int[] {10, 0}, data.stock(productId));
        assertEquals("REFUNDED", data.paymentStatus(orderNumber));
    }

    private long stockedProduct() {
        TestData.Store store = data.store();
        return data.product(store, new BigDecimal("100.00"), new BigDecimal("5.00"), 10);
    }

    private String place(long userId, long productId) {
        data.addToCart(userId, productId, 1);
        return checkout.place(userId, UUID.randomUUID().toString(), null, null).get(0).getOrderNumber();
    }
}
