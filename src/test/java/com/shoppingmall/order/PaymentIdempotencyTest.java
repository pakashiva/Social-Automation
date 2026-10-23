package com.shoppingmall.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shoppingmall.dto.InvoiceView;
import com.shoppingmall.enums.SimulatedOutcome;
import com.shoppingmall.repository.InvoiceRepository;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.service.PaymentService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentIdempotencyTest extends MallSpringTest {

    @Autowired
    private CheckoutService checkout;

    @Autowired
    private PaymentService payments;

    @Autowired
    private InvoiceRepository invoices;

    @Test
    void aSecondSuccessReturnsTheSameInvoice() {
        TestData.Store store = data.store();
        long productId = data.product(store, new BigDecimal("100.00"), new BigDecimal("5.00"), 10);
        TestData.Person customer = data.customer();
        data.addToCart(customer.userId(), productId, 1);
        String orderNumber = checkout.place(customer.userId(), UUID.randomUUID().toString(), null, null)
                .get(0).getOrderNumber();

        InvoiceView first = payments.pay(customer.userId(), orderNumber, SimulatedOutcome.SUCCESS);
        InvoiceView second = payments.pay(customer.userId(), orderNumber, SimulatedOutcome.SUCCESS);

        assertEquals(first.getInvoiceNumber(), second.getInvoiceNumber());
        assertEquals(1L, invoices.countByOrderId(data.orderId(orderNumber)));
    }
}
