package com.shoppingmall.service;

import com.shoppingmall.dto.InvoiceView;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.enums.SimulatedOutcome;
import org.springframework.data.domain.Page;

public interface PaymentService {

    InvoiceView pay(long userId, String orderNumber, SimulatedOutcome outcome);

    Page<Rows.PaymentRow> search(int page);
}
