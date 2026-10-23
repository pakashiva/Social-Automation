package com.shoppingmall.service;

import com.shoppingmall.dto.Rows;
import java.util.List;

public interface WishlistService {

    List<Rows.WishlistRow> view(long userId);

    void add(long userId, long productId);

    void remove(long userId, long itemId);

    void moveToCart(long userId, long itemId);
}
