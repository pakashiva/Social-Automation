package com.shoppingmall.service;

import com.shoppingmall.dto.ComplaintForm;
import com.shoppingmall.dto.Rows;
import org.springframework.data.domain.Page;

public interface ComplaintService {

    Page<Rows.ComplaintRow> listForCustomer(long userId, int page);

    Rows.ComplaintRow getForCustomer(long userId, String complaintNumber);

    void create(long userId, ComplaintForm form);

    Page<Rows.ComplaintRow> listAll(int page);

    Rows.ComplaintRow get(String complaintNumber);

    void assign(String complaintNumber, long assigneeId);

    void resolve(long adminId, String complaintNumber, String resolutionNote);

    void reject(long adminId, String complaintNumber, String resolutionNote);
}
