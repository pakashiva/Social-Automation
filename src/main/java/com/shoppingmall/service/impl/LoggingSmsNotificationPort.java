package com.shoppingmall.service.impl;

import com.shoppingmall.service.SmsNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSmsNotificationPort implements SmsNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsNotificationPort.class);

    @Override
    public void sendCopy(long userId, String mobile, String body) {
        log.info("sms copy userId={} mobile={}", userId, mobile);
    }
}
