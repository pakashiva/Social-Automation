package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.InvoiceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InvoiceController {

    private final InvoiceService invoices;

    public InvoiceController(InvoiceService invoices) {
        this.invoices = invoices;
    }

    @GetMapping("/customer/invoices")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("invoices", invoices.listForCustomer(MallUserDetails.requireId(authentication), page));
        return "customer/invoices";
    }

    @GetMapping("/customer/invoices/{invoiceNumber}")
    public String details(Authentication authentication, @PathVariable String invoiceNumber, Model model) {
        model.addAttribute("invoice", invoices.getForCustomer(MallUserDetails.requireId(authentication), invoiceNumber));
        return "customer/invoice-details";
    }
}
