package com.shoppingmall.service;

public interface SmsNotificationPort {

    void sendCopy(long userId, String mobile, String body);
}
