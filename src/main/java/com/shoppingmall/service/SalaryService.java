package com.shoppingmall.service;

import com.shoppingmall.dto.Rows;
import org.springframework.data.domain.Page;

public interface SalaryService {

    Page<Rows.SalaryRow> list(int page);

    String generate(long adminId, String yearMonth);

    void markPaid(long adminId, long id);
}
