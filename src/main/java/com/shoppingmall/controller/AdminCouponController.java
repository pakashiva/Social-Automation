package com.shoppingmall.controller;

import com.shoppingmall.dto.CouponForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.CouponService;
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
public class AdminCouponController {

    private final CouponService coupons;

    public AdminCouponController(CouponService coupons) {
        this.coupons = coupons;
    }

    @GetMapping("/admin/coupons")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("coupons", coupons.listAll(page));
        return "admin/coupons";
    }

    @GetMapping("/admin/coupons/new")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CouponForm());
        }
        model.addAttribute("choices", coupons.formData());
        return "admin/add-coupon";
    }

    @PostMapping("/admin/coupons")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("form") CouponForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("choices", coupons.formData());
            return "admin/add-coupon";
        }
        coupons.create(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Coupon saved.");
        return "redirect:/admin/coupons";
    }

    @PostMapping("/admin/coupons/{id}/deactivate")
    public String deactivate(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        coupons.deactivate(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Coupon deactivated.");
        return "redirect:/admin/coupons";
    }
}
