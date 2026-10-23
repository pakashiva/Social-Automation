package com.shoppingmall.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Pages {

    public static final int SIZE = 10;

    private Pages() {
    }

    public static Pageable of(int page) {
        return PageRequest.of(Math.max(page, 0), SIZE);
    }
}
