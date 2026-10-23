package com.shoppingmall.service.impl;

import com.shoppingmall.dto.NotificationSendForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Notification;
import com.shoppingmall.entity.Product;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.NotificationAudience;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.ForbiddenException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.NotificationRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.EmailNotificationPort;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.SmsNotificationPort;
import com.shoppingmall.util.Pages;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final InAppNotificationWriter writer;
    private final EmailNotificationPort email;
    private final SmsNotificationPort sms;
    private final AuditService audit;
    private final Clock clock;

    public NotificationServiceImpl(
            NotificationRepository notifications,
            UserRepository users,
            InAppNotificationWriter writer,
            EmailNotificationPort email,
            SmsNotificationPort sms,
            AuditService audit,
            Clock clock) {
        this.notifications = notifications;
        this.users = users;
        this.writer = writer;
        this.email = email;
        this.sms = sms;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void notifyUser(long userId, NotificationType type, String title, String body, String linkPath) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User was not found."));
        writer.insert(user, type, title, body, linkPath);
        email.sendCopy(userId, title, body);
        sms.sendCopy(userId, user.getMobile(), body);
    }

    @Override
    @Transactional
    public void notifyLowStock(Product product, int onHand) {
        String title = "Low stock";
        String body = product.getName() + " now has " + onHand + " on hand.";
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(product.getShop().getOwner().getUser().getId());
        for (User admin : users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.ADMIN)) {
            recipients.add(admin.getId());
        }
        for (Long userId : recipients) {
            String link = userId.equals(product.getShop().getOwner().getUser().getId())
                    ? "/owner/inventory"
                    : "/admin/products";
            notifyUser(userId, NotificationType.LOW_STOCK, title, body, link);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.NotificationRow> listMine(long userId, int page) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.NotificationRow> listAll(int page) {
        return notifications.findAllByOrderByCreatedAtDesc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional
    public void markRead(long userId, long notificationId) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification was not found."));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException();
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(clock));
        }
    }

    @Override
    @Transactional
    public void broadcast(long adminId, NotificationSendForm form) {
        List<User> recipients;
        if (form.getAudience() == NotificationAudience.ONE_USER) {
            if (form.getUserId() == null || users.findById(form.getUserId()).isEmpty()) {
                throw new BusinessException("Choose a user.", "/admin/notifications");
            }
            recipients = List.of(users.findById(form.getUserId()).orElseThrow());
        } else if (form.getAudience() == NotificationAudience.ALL_CUSTOMERS) {
            recipients = users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.CUSTOMER);
        } else if (form.getAudience() == NotificationAudience.ALL_OWNERS) {
            recipients = users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.SHOP_OWNER);
        } else {
            recipients = users.findByStatusAndRole(UserStatus.ACTIVE, RoleName.ADMIN);
        }
        for (User user : recipients) {
            notifyUser(user.getId(), NotificationType.GENERAL, form.getTitle(), form.getBody(), "/");
        }
        audit.record(adminId, "NOTIFICATION_SENT", "notifications", null,
                "Broadcast " + form.getAudience() + " to " + recipients.size() + " users.");
    }

    private Rows.NotificationRow row(Notification notification) {
        Rows.NotificationRow row = new Rows.NotificationRow();
        row.setId(notification.getId());
        row.setType(notification.getType().name());
        row.setTitle(notification.getTitle());
        row.setBody(notification.getBody());
        row.setLinkPath(notification.getLinkPath());
        row.setUnread(notification.getReadAt() == null);
        row.setCreatedAt(notification.getCreatedAt());
        row.setUserEmail(notification.getUser().getEmail());
        return row;
    }
}
