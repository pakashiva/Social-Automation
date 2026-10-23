package com.shoppingmall.service;

import com.shoppingmall.dto.CouponForm;
import com.shoppingmall.dto.Rows;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CouponService {

    List<Rows.CouponRow> listPublic();

    Page<Rows.CouponRow> listForOwner(long userId, int page);

    Page<Rows.CouponRow> listAll(int page);

    Rows.CouponChoices formData();

    void createShopCoupon(long userId, CouponForm form);

    void deactivateOwn(long userId, long id);

    void create(long adminId, CouponForm form);

    void deactivate(long adminId, long id);
}
