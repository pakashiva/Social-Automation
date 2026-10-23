package com.shoppingmall.controller;

import com.shoppingmall.dto.CancelOrderForm;
import com.shoppingmall.dto.OrderStatusForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.OrderService;
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
public class OwnerOrderController {

    private final OrderService orders;

    public OwnerOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/owner/orders")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orders.listForOwner(MallUserDetails.requireId(authentication), page));
        return "owner/orders";
    }

    @GetMapping("/owner/orders/{orderNumber}")
    public String details(Authentication authentication, @PathVariable String orderNumber, Model model) {
        model.addAttribute("order", orders.getForOwner(MallUserDetails.requireId(authentication), orderNumber));
        return "owner/order-details";
    }

    @PostMapping("/owner/orders/{orderNumber}/status")
    public String status(
            Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @ModelAttribute OrderStatusForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Choose a status.");
            return "redirect:/owner/orders/" + orderNumber;
        }
        orders.changeStatus(MallUserDetails.requireId(authentication), orderNumber, form.getStatus());
        redirectAttributes.addFlashAttribute("success", "Order status updated.");
        return "redirect:/owner/orders/" + orderNumber;
    }

    @PostMapping("/owner/orders/{orderNumber}/cancel")
    public String cancel(
            Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @ModelAttribute CancelOrderForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Write a reason of at least 5 characters.");
            return "redirect:/owner/orders/" + orderNumber;
        }
        orders.cancelByOwner(MallUserDetails.requireId(authentication), orderNumber, form.getCancelReason());
        redirectAttributes.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/owner/orders/" + orderNumber;
    }
}
