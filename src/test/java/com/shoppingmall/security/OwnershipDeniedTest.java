package com.shoppingmall.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.exception.ForbiddenException;
import com.shoppingmall.service.CheckoutService;
import com.shoppingmall.service.OrderService;
import com.shoppingmall.service.ProductService;
import com.shoppingmall.service.ShopService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OwnershipDeniedTest extends MallSpringTest {

    @Autowired
    private ShopService shops;

    @Autowired
    private ProductService products;

    @Autowired
    private OrderService orders;

    @Autowired
    private CheckoutService checkout;

    @Test
    void anotherOwnerCannotOpenTheShopProductOrOrder() {
        TestData.Store owner = data.store();
        TestData.Store other = data.store();
        long productId = data.product(owner, new BigDecimal("100.00"), new BigDecimal("5.00"), 8);
        TestData.Person customer = data.customer();
        data.addToCart(customer.userId(), productId, 1);
        String orderNumber = checkout.place(customer.userId(), UUID.randomUUID().toString(), null, null)
                .get(0).getOrderNumber();

        assertThrows(ForbiddenException.class, () -> shops.updateOwn(other.userId(), owner.shopId(), null));
        assertThrows(ForbiddenException.class, () -> products.getForOwner(other.userId(), productId));
        assertThrows(ForbiddenException.class, () -> orders.getForOwner(other.userId(), orderNumber));
    }
}
