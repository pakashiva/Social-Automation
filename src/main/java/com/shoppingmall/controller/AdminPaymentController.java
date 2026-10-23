package com.shoppingmall.controller;

import com.shoppingmall.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminPaymentController {

    private final PaymentService payments;

    public AdminPaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @GetMapping("/admin/payments")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("payments", payments.search(page));
        return "admin/payments";
    }
}
