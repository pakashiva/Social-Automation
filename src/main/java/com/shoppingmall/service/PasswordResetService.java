package com.shoppingmall.service;

public interface PasswordResetService {

    void issue(String email);

    boolean peek(String token);

    void reset(String token, String password, String confirmPassword);
}
