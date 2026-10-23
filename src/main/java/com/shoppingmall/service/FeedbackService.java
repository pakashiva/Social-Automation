package com.shoppingmall.service;

import com.shoppingmall.dto.ContactForm;
import com.shoppingmall.dto.FeedbackForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.enums.FeedbackStatus;
import org.springframework.data.domain.Page;

public interface FeedbackService {

    Page<Rows.FeedbackRow> listForCustomer(long userId, int page);

    Page<Rows.FeedbackRow> listAll(int page);

    void submitProfile(long userId, FeedbackForm form);

    void submitContact(ContactForm form);

    void changeStatus(long id, FeedbackStatus status);
}
