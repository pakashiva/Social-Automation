package com.shoppingmall.exception;

public class BusinessException extends RuntimeException {

    private final String redirectPath;
    private final int httpStatus;

    public BusinessException(String message) {
        this(message, null, 0);
    }

    public BusinessException(String message, String redirectPath) {
        this(message, redirectPath, 0);
    }

    public BusinessException(String message, String redirectPath, int httpStatus) {
        super(message);
        this.redirectPath = redirectPath;
        this.httpStatus = httpStatus;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
