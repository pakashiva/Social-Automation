package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ShopCard;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.dto.ShopProfileForm;
import com.shoppingmall.entity.Shop;
import com.shoppingmall.entity.ShopOwner;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.ApprovalStatus;
import com.shoppingmall.enums.ShopStatus;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.ShopRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.NotificationService;
import com.shoppingmall.service.ShopService;
import com.shoppingmall.enums.NotificationType;
import com.shoppingmall.util.Pages;
import com.shoppingmall.validation.MobileNumber;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shops;
    private final OwnershipGuard guard;
    private final AuditService audit;
    private final NotificationService notifications;
    private final ImageFiles images;
    private final Clock clock;

    public ShopServiceImpl(
            ShopRepository shops,
            OwnershipGuard guard,
            AuditService audit,
            NotificationService notifications,
            ImageFiles images,
            Clock clock) {
        this.shops = shops;
        this.guard = guard;
        this.audit = audit;
        this.notifications = notifications;
        this.images = images;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopCard> featured() {
        return shops.findByStatusOrderByNameAsc(ShopStatus.ACTIVE).stream().limit(8).map(this::card).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopCard> listPublic(int page) {
        return shops.findByStatus(ShopStatus.ACTIVE, Pages.of(page)).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopCard getPublicBySlug(String slug) {
        Shop shop = shops.findBySlug(slug).filter(row -> row.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Shop was not found."));
        return card(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopCard> listAll(int page, ShopStatus status) {
        if (status == null) {
            return shops.findAllByOrderByNameAsc(Pages.of(page)).map(this::card);
        }
        return shops.findByStatus(status, Pages.of(page)).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopCard get(long id) {
        return card(shops.findById(id).orElseThrow(() -> new NotFoundException("Shop was not found.")));
    }

    @Override
    @Transactional
    public void approve(long adminId, long shopId) {
        Shop shop = requirePending(shopId);
        ShopOwner owner = shop.getOwner();
        User user = owner.getUser();
        user.setStatus(UserStatus.ACTIVE);
        owner.setApprovalStatus(ApprovalStatus.APPROVED);
        owner.setApprovedBy(guard.requireUser(adminId));
        owner.setApprovedAt(LocalDateTime.now(clock));
        owner.setRejectionReason(null);
        shop.setStatus(ShopStatus.ACTIVE);
        audit.record(adminId, "SHOP_APPROVED", "shops", shopId, "Approved " + shop.getName() + ".");
        notifications.notifyUser(user.getId(), NotificationType.APPROVAL, "Shop approved",
                "Your shop " + shop.getName() + " is approved.", "/owner/dashboard");
    }

    @Override
    @Transactional
    public void reject(long adminId, long shopId, String rejectionReason) {
        String reason = rejectionReason == null ? "" : rejectionReason.trim();
        if (reason.length() < 10 || reason.length() > 500) {
            throw new BusinessException("Enter a rejection reason of 10 to 500 characters.", "/admin/shops/" + shopId);
        }
        Shop shop = requirePending(shopId);
        shop.getOwner().setApprovalStatus(ApprovalStatus.REJECTED);
        shop.getOwner().setRejectionReason(reason);
        shop.getOwner().getUser().setStatus(UserStatus.REJECTED);
        shop.setStatus(ShopStatus.DRAFT);
        audit.record(adminId, "SHOP_REJECTED", "shops", shopId, "Rejected " + shop.getName() + ".");
    }

    @Override
    @Transactional
    public void changeStatus(long adminId, long shopId, ShopStatus status) {
        if (status != ShopStatus.ACTIVE && status != ShopStatus.SUSPENDED && status != ShopStatus.CLOSED) {
            throw new BusinessException("Choose active, suspended, or closed.", "/admin/shops/" + shopId);
        }
        Shop shop = shops.findById(shopId).orElseThrow(() -> new NotFoundException("Shop was not found."));
        if (shop.getOwner().getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Only an approved shop can change status.", "/admin/shops/" + shopId);
        }
        shop.setStatus(status);
        audit.record(adminId, "SHOP_STATUS_CHANGED", "shops", shopId, "Shop status set to " + status + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopCard> listForOwner(long userId) {
        ShopOwner owner = guard.requireOwner(userId);
        return shops.findByOwnerId(owner.getId()).stream().map(this::card).toList();
    }

    @Override
    @Transactional
    public void updateOwn(long userId, long shopId, ShopProfileForm form) {
        Shop shop = guard.requireOwnShop(userId, shopId);
        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new BusinessException("This shop is closed.", "/owner/shop-profile");
        }
        if (form.getOpeningTime() == null || form.getClosingTime() == null
                || !form.getOpeningTime().isBefore(form.getClosingTime())) {
            throw new BusinessException("Closing time must be after opening time.", "/owner/shop-profile");
        }
        if (!FieldRules.mobile(form.getContactPhone())) {
            throw new BusinessException("Enter a 10-digit mobile number starting with 6, 7, 8, or 9.", "/owner/shop-profile");
        }
        shop.setDescription(form.getDescription());
        shop.setContactPhone(form.getContactPhone().trim());
        shop.setOpeningTime(form.getOpeningTime());
        shop.setClosingTime(form.getClosingTime());
        if (form.getLogo() != null && !form.getLogo().isEmpty()) {
            try {
                shop.setLogoPath(images.store("shops", shop.getId(), form.getLogo().getBytes()));
            } catch (Exception exception) {
                String message = exception instanceof BusinessException business
                        ? business.getMessage()
                        : "Upload a JPEG, PNG, or WEBP image within the size limit.";
                throw new BusinessException(message, "/owner/shop-profile");
            }
        }
    }

    private Shop requirePending(long shopId) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new NotFoundException("Shop was not found."));
        if (shop.getStatus() != ShopStatus.DRAFT || shop.getOwner().getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("This application has already been decided.", "/admin/shops/" + shopId);
        }
        return shop;
    }

    private ShopCard card(Shop shop) {
        ShopCard card = new ShopCard();
        card.setId(shop.getId());
        card.setName(shop.getName());
        card.setSlug(shop.getSlug());
        card.setDescription(shop.getDescription());
        card.setFloorLabel(shop.getFloorLabel());
        card.setUnitNumber(shop.getUnitNumber());
        card.setPhone(shop.getContactPhone());
        card.setEmail(shop.getContactEmail());
        card.setHours(shop.getOpeningTime() + " to " + shop.getClosingTime());
        card.setStatus(shop.getStatus().name());
        card.setLogoUrl(logoUrl(shop));
        card.setOwnerName(shop.getOwner().getUser().getFullName());
        card.setBusinessName(shop.getOwner().getBusinessName());
        card.setGstNumber(shop.getOwner().getGstNumber());
        card.setApprovalStatus(shop.getOwner().getApprovalStatus().name());
        return card;
    }

    static String logoUrl(Shop shop) {
        if (shop.getLogoPath() == null || shop.getLogoPath().isBlank()) {
            return "/images/shop.svg";
        }
        String file = shop.getLogoPath().substring(shop.getLogoPath().lastIndexOf('/') + 1);
        return "/media/shops/" + shop.getId() + "/" + file;
    }
}
