package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ReviewForm;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.entity.Review;
import com.shoppingmall.enums.OrderStatus;
import com.shoppingmall.enums.ReviewStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.OrderItemRepository;
import com.shoppingmall.repository.ReviewRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final OwnershipGuard guard;
    private final OrderItemRepository orderItems;
    private final ReviewRepository reviews;
    private final AuditService audit;

    public ReviewServiceImpl(
            OwnershipGuard guard,
            OrderItemRepository orderItems,
            ReviewRepository reviews,
            AuditService audit) {
        this.guard = guard;
        this.orderItems = orderItems;
        this.reviews = reviews;
        this.audit = audit;
    }

    @Override
    @Transactional
    public void create(long userId, ReviewForm form) {
        guard.requireCustomer(userId);
        if (form.getOrderItemId() == null) {
            throw new BusinessException("You can review this item after it is delivered.", "/customer/orders");
        }
        OrderItem item = orderItems.findById(form.getOrderItemId())
                .orElseThrow(() -> new BusinessException("You can review this item after it is delivered.", "/customer/orders"));
        String orderNumber = item.getOrder().getOrderNumber();
        String back = "/customer/orders/" + orderNumber;
        if (!item.getOrder().getCustomer().getUser().getId().equals(userId)
                || item.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("You can review this item after it is delivered.", back);
        }
        if (reviews.existsByOrderItemId(item.getId())) {
            throw new BusinessException("You have already reviewed this item.", back);
        }
        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5) {
            throw new BusinessException("Choose a rating from 1 to 5.", back);
        }
        if (form.getTitle() == null || form.getTitle().trim().length() < 3 || form.getTitle().trim().length() > 120) {
            throw new BusinessException("Write a title of at least 3 characters.", back);
        }
        if (form.getBody() == null || form.getBody().trim().length() < 10 || form.getBody().trim().length() > 2000) {
            throw new BusinessException("Write at least 10 characters.", back);
        }
        Review review = new Review();
        review.setProduct(item.getProduct());
        review.setCustomer(item.getOrder().getCustomer());
        review.setOrderItem(item);
        review.setRating(form.getRating());
        review.setTitle(form.getTitle().trim());
        review.setBody(form.getBody().trim());
        review.setStatus(ReviewStatus.PUBLISHED);
        reviews.save(review);
    }

    @Override
    @Transactional
    public void hide(long adminId, long id) {
        Review review = reviews.findById(id).orElseThrow(() -> new NotFoundException("Review was not found."));
        review.setStatus(ReviewStatus.HIDDEN);
        reviews.save(review);
        audit.record(adminId, "REVIEW_HIDDEN", "reviews", review.getId(), "Hid review " + review.getId());
    }
}
