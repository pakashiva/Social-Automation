package com.shoppingmall.exception;

public class DuplicateAccountException extends RuntimeException {

    private final String field;

    public DuplicateAccountException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
