package com.shoppingmall.service;

import com.shoppingmall.dto.ReviewForm;

public interface ReviewService {

    void create(long userId, ReviewForm form);

    void hide(long adminId, long id);
}
