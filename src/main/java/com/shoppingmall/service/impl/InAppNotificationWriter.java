package com.shoppingmall.service.impl;

import com.shoppingmall.entity.Notification;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationWriter {

    private final NotificationRepository notifications;

    public InAppNotificationWriter(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public Notification insert(User user, NotificationType type, String title, String body, String linkPath) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(trim(title, 160));
        notification.setBody(trim(body, 1000));
        notification.setLinkPath(linkPath);
        return notifications.save(notification);
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
