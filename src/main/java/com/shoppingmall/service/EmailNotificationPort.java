package com.shoppingmall.service;

public interface EmailNotificationPort {

    void sendPasswordReset(long userId, String rawToken);

    void sendCopy(long userId, String title, String body);
}
