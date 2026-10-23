package com.shoppingmall.service;

import com.shoppingmall.dto.NotificationSendForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Product;
import com.shoppingmall.enums.NotificationType;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void notifyUser(long userId, NotificationType type, String title, String body, String linkPath);

    void notifyLowStock(Product product, int onHand);

    Page<Rows.NotificationRow> listMine(long userId, int page);

    Page<Rows.NotificationRow> listAll(int page);

    void markRead(long userId, long notificationId);

    void broadcast(long adminId, NotificationSendForm form);
}
