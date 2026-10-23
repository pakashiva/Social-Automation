package com.shoppingmall.controller;

import com.shoppingmall.dto.CheckoutForm;
import com.shoppingmall.dto.OrderView;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.CheckoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    static final String TOKEN = "checkoutToken";

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @GetMapping("/customer/checkout")
    public String preview(
            Authentication authentication,
            @RequestParam(required = false) String couponCode,
            HttpSession session,
            Model model) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(TOKEN, token);
        model.addAttribute("checkoutToken", token);
        model.addAttribute("cart", checkout.preview(MallUserDetails.requireId(authentication), couponCode));
        model.addAttribute("couponCode", couponCode == null ? "" : couponCode);
        return "customer/checkout";
    }

    @PostMapping("/customer/checkout")
    public String place(
            Authentication authentication,
            @Valid @ModelAttribute CheckoutForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Object expected = session.getAttribute(TOKEN);
        boolean tokenMatches = expected != null && form.getCheckoutToken() != null
                && form.getCheckoutToken().equals(expected.toString());
        if (!tokenMatches) {
            throw new BusinessException("Checkout was opened in another tab. Start checkout again.", null, 400);
        }
        if (binding.hasErrors()) {
            model.addAttribute("checkoutToken", expected);
            model.addAttribute("cart", checkout.preview(MallUserDetails.requireId(authentication), form.getCouponCode()));
            model.addAttribute("couponCode", form.getCouponCode() == null ? "" : form.getCouponCode());
            return "customer/checkout";
        }
        List<OrderView> placed = checkout.place(
                MallUserDetails.requireId(authentication),
                form.getCheckoutToken(),
                form.getCouponCode(),
                form.getCustomerNote());
        session.removeAttribute(TOKEN);
        if (placed.size() == 1) {
            return "redirect:/customer/orders/" + placed.get(0).getOrderNumber();
        }
        redirectAttributes.addFlashAttribute("success", "Your order was placed.");
        return "redirect:/customer/orders";
    }
}
