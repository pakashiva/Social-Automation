package com.shoppingmall.controller;

import com.shoppingmall.dto.CancelOrderForm;
import com.shoppingmall.dto.PayForm;
import com.shoppingmall.dto.ReviewForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.OrderService;
import com.shoppingmall.service.PaymentService;
import com.shoppingmall.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerOrderController {

    private final OrderService orders;
    private final PaymentService payments;
    private final ReviewService reviews;

    public CustomerOrderController(OrderService orders, PaymentService payments, ReviewService reviews) {
        this.orders = orders;
        this.payments = payments;
        this.reviews = reviews;
    }

    @GetMapping("/customer/orders")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orders.listForCustomer(MallUserDetails.requireId(authentication), page));
        return "customer/orders";
    }

    @GetMapping("/customer/orders/{orderNumber}")
    public String details(Authentication authentication, @PathVariable String orderNumber, Model model) {
        model.addAttribute("order", orders.getForCustomer(MallUserDetails.requireId(authentication), orderNumber));
        if (!model.containsAttribute("payForm")) {
            model.addAttribute("payForm", new PayForm());
        }
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelOrderForm());
        }
        if (!model.containsAttribute("reviewForm")) {
            model.addAttribute("reviewForm", new ReviewForm());
        }
        return "customer/order-details";
    }

    @PostMapping("/customer/orders/{orderNumber}/pay")
    public String pay(
            Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @ModelAttribute PayForm form,
            BindingResult binding) {
        if (binding.hasErrors()) {
            return "redirect:/customer/orders/" + orderNumber;
        }
        var invoice = payments.pay(MallUserDetails.requireId(authentication), orderNumber, form.getOutcome());
        return "redirect:/customer/invoices/" + invoice.getInvoiceNumber();
    }

    @PostMapping("/customer/orders/{orderNumber}/cancel")
    public String cancel(
            Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @ModelAttribute CancelOrderForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Write a reason of at least 5 characters.");
            return "redirect:/customer/orders/" + orderNumber;
        }
        orders.cancelByCustomer(MallUserDetails.requireId(authentication), orderNumber, form.getCancelReason());
        redirectAttributes.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/customer/orders/" + orderNumber;
    }
}
