package com.shoppingmall.service.impl;

import com.shoppingmall.dto.InvoiceView;
import com.shoppingmall.dto.OrderView;
import com.shoppingmall.entity.Invoice;
import com.shoppingmall.entity.OrderItem;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.InvoiceRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.InvoiceService;
import com.shoppingmall.util.Pages;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoices;
    private final OwnershipGuard guard;

    public InvoiceServiceImpl(InvoiceRepository invoices, OwnershipGuard guard) {
        this.invoices = invoices;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceView> listForCustomer(long userId, int page) {
        return invoices.findByCustomerIdOrderByIssuedAtDesc(guard.requireCustomer(userId).getId(), Pages.of(page))
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceView getForCustomer(long userId, String invoiceNumber) {
        Invoice invoice = invoices.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new NotFoundException("Invoice was not found."));
        if (!invoice.getCustomer().getId().equals(guard.requireCustomer(userId).getId())) {
            throw new NotFoundException("Invoice was not found.");
        }
        return toView(invoice);
    }

    private InvoiceView toView(Invoice invoice) {
        InvoiceView view = new InvoiceView();
        view.setInvoiceNumber(invoice.getInvoiceNumber());
        view.setOrderNumber(invoice.getOrder().getOrderNumber());
        view.setShopName(invoice.getShop().getName());
        view.setBilledName(invoice.getBilledName());
        view.setBilledEmail(invoice.getBilledEmail());
        view.setBilledMobile(invoice.getBilledMobile());
        view.setBilledAddress(invoice.getBilledAddress());
        view.setSubtotal(invoice.getSubtotal());
        view.setDiscount(invoice.getDiscountAmount());
        view.setTax(invoice.getTaxAmount());
        view.setTotal(invoice.getTotalAmount());
        view.setIssuedAt(invoice.getIssuedAt());
        for (OrderItem item : invoice.getOrder().getItems()) {
            OrderView.Line line = new OrderView.Line();
            line.setProductName(item.getProductNameSnapshot());
            line.setSku(item.getSkuSnapshot());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            line.setDiscount(item.getDiscountAmount());
            line.setTax(item.getTaxAmount());
            line.setLineTotal(item.getLineTotal());
            view.getLines().add(line);
        }
        return view;
    }
}
