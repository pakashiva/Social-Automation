package com.shoppingmall.service.impl;

import com.shoppingmall.dto.ContactForm;
import com.shoppingmall.dto.FeedbackForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Feedback;
import com.shoppingmall.enums.FeedbackSource;
import com.shoppingmall.enums.FeedbackStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.FeedbackRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.FeedbackService;
import com.shoppingmall.util.Pages;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedback;
    private final OwnershipGuard guard;

    public FeedbackServiceImpl(FeedbackRepository feedback, OwnershipGuard guard) {
        this.feedback = feedback;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.FeedbackRow> listForCustomer(long userId, int page) {
        return feedback.findByCustomerIdOrderByCreatedAtDesc(guard.requireCustomer(userId).getId(), Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.FeedbackRow> listAll(int page) {
        return feedback.findAllByOrderByCreatedAtDesc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional
    public void submitProfile(long userId, FeedbackForm form) {
        if (form.getMessage() == null || form.getMessage().trim().length() < 10) {
            throw new BusinessException("Write at least 10 characters.", "/customer/feedback");
        }
        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5) {
            throw new BusinessException("Choose a rating from 1 to 5.", "/customer/feedback");
        }
        Feedback row = new Feedback();
        row.setCustomer(guard.requireCustomer(userId));
        row.setSource(FeedbackSource.PROFILE);
        row.setGuestName(null);
        row.setGuestEmail(null);
        row.setSubject(form.getSubject().trim());
        row.setMessage(form.getMessage().trim());
        row.setRating(form.getRating());
        row.setStatus(FeedbackStatus.OPEN);
        feedback.save(row);
    }

    @Override
    @Transactional
    public void submitContact(ContactForm form) {
        if (form.getMessage() == null || form.getMessage().trim().length() < 10) {
            throw new BusinessException("Write at least 10 characters.", "/contact");
        }
        Feedback row = new Feedback();
        row.setCustomer(null);
        row.setSource(FeedbackSource.CONTACT);
        row.setGuestName(form.getGuestName().trim());
        row.setGuestEmail(form.getGuestEmail().trim().toLowerCase());
        row.setSubject(form.getSubject().trim());
        row.setMessage(form.getMessage().trim());
        row.setRating(null);
        row.setStatus(FeedbackStatus.OPEN);
        feedback.save(row);
    }

    @Override
    @Transactional
    public void changeStatus(long id, FeedbackStatus status) {
        Feedback row = feedback.findById(id).orElseThrow(() -> new NotFoundException("Feedback was not found."));
        if (status == null) {
            throw new BusinessException("Choose a status.", "/admin/customers");
        }
        row.setStatus(status);
        feedback.save(row);
    }

    private Rows.FeedbackRow row(Feedback source) {
        Rows.FeedbackRow row = new Rows.FeedbackRow();
        row.setId(source.getId());
        row.setSource(source.getSource().name());
        row.setSubject(source.getSubject());
        row.setMessage(source.getMessage());
        row.setRating(source.getRating());
        row.setStatus(source.getStatus().name());
        if (source.getCustomer() != null) {
            row.setFromName(source.getCustomer().getUser().getFullName());
        } else {
            row.setFromName(source.getGuestName());
        }
        return row;
    }
}
