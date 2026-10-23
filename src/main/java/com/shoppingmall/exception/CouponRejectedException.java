package com.shoppingmall.exception;

import com.shoppingmall.enums.CouponRejectReason;

public class CouponRejectedException extends BusinessException {

    private final CouponRejectReason reason;

    public CouponRejectedException(CouponRejectReason reason, String message) {
        super(message, "/customer/checkout");
        this.reason = reason;
    }

    public CouponRejectReason getReason() {
        return reason;
    }
}
