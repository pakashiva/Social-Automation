package com.shoppingmall.service;

import com.shoppingmall.dto.InvoiceView;
import org.springframework.data.domain.Page;

public interface InvoiceService {

    Page<InvoiceView> listForCustomer(long userId, int page);

    InvoiceView getForCustomer(long userId, String invoiceNumber);
}
