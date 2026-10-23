package com.shoppingmall.service;

import com.shoppingmall.dto.CartView;
import com.shoppingmall.dto.OrderView;
import java.util.List;

public interface CheckoutService {

    CartView preview(long userId, String couponCode);

    List<OrderView> place(long userId, String checkoutToken, String couponCode, String customerNote);
}
