package com.shoppingmall.controller;

import com.shoppingmall.dto.CancelOrderForm;
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
public class AdminOrderController {

    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/admin/orders")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orders.listAll(page));
        return "admin/orders";
    }

    @GetMapping("/admin/orders/{orderNumber}")
    public String details(@PathVariable String orderNumber, Model model) {
        model.addAttribute("order", orders.get(orderNumber));
        return "admin/order-details";
    }

    @PostMapping("/admin/orders/{orderNumber}/cancel")
    public String cancel(
            Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @ModelAttribute CancelOrderForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Write a reason of at least 5 characters.");
            return "redirect:/admin/orders/" + orderNumber;
        }
        orders.cancelByAdmin(MallUserDetails.requireId(authentication), orderNumber, form.getCancelReason());
        redirectAttributes.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/admin/orders/" + orderNumber;
    }
}
