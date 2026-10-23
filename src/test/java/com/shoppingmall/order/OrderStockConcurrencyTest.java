package com.shoppingmall.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.shoppingmall.dto.OrderView;
import com.shoppingmall.exception.InsufficientStockException;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderStockConcurrencyTest extends MallSpringTest {

    @Autowired
    private CheckoutService checkout;

    @Test
    void onlyOneCheckoutCanReserveTheLastUnit() throws Exception {
        TestData.Store store = data.store();
        long productId = data.product(store, new BigDecimal("100.00"), new BigDecimal("5.00"), 1);
        TestData.Person firstCustomer = data.customer();
        TestData.Person secondCustomer = data.customer();
        data.addToCart(firstCustomer.userId(), productId, 1);
        data.addToCart(secondCustomer.userId(), productId, 1);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<OrderView> first = pool.submit(() -> place(barrier, firstCustomer.userId()));
            Future<OrderView> second = pool.submit(() -> place(barrier, secondCustomer.userId()));
            int reserved = 0;
            int rejected = 0;
            for (Future<OrderView> future : java.util.List.of(first, second)) {
                try {
                    future.get(40, TimeUnit.SECONDS);
                    reserved++;
                } catch (java.util.concurrent.ExecutionException exception) {
                    assertInstanceOf(InsufficientStockException.class, exception.getCause(), String.valueOf(exception.getCause()));
                    rejected++;
                }
            }
            assertEquals(1, reserved);
            assertEquals(1, rejected);
        } finally {
            pool.shutdownNow();
        }
    }

    private OrderView place(CyclicBarrier barrier, long userId) throws Exception {
        barrier.await(20, TimeUnit.SECONDS);
        return checkout.place(userId, UUID.randomUUID().toString(), null, null).get(0);
    }
}
