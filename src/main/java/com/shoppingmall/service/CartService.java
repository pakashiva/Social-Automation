package com.shoppingmall.service;

import com.shoppingmall.dto.CartView;

public interface CartService {

    CartView view(long userId);

    void add(long userId, long productId, int quantity);

    void updateQuantity(long userId, long itemId, int quantity);

    void remove(long userId, long itemId);
}
