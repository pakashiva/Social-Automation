package com.shoppingmall.service.impl;

import com.shoppingmall.service.EmailNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailNotificationPort implements EmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationPort.class);

    private final boolean logResetToken;

    public LoggingEmailNotificationPort(@Value("${app.mail.log-reset-token:false}") boolean logResetToken) {
        this.logResetToken = logResetToken;
    }

    @Override
    public void sendPasswordReset(long userId, String rawToken) {
        if (logResetToken) {
            log.info("password reset userId={} path=/reset-password?token={}", userId, rawToken);
        } else {
            log.info("password reset issued userId={}", userId);
        }
    }

    @Override
    public void sendCopy(long userId, String title, String body) {
        log.info("email copy userId={} title={}", userId, title);
    }
}
