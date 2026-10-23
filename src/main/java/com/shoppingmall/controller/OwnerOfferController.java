package com.shoppingmall.controller;

import com.shoppingmall.dto.CouponForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.CouponService;
import com.shoppingmall.service.ShopService;
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
public class OwnerOfferController {

    private final CouponService coupons;
    private final ShopService shops;

    public OwnerOfferController(CouponService coupons, ShopService shops) {
        this.coupons = coupons;
        this.shops = shops;
    }

    @GetMapping("/owner/offers")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("offers", coupons.listForOwner(MallUserDetails.requireId(authentication), page));
        return "owner/offers";
    }

    @GetMapping("/owner/offers/new")
    public String form(Authentication authentication, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CouponForm());
        }
        model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
        return "owner/add-offer";
    }

    @PostMapping("/owner/offers")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("form") CouponForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
            return "owner/add-offer";
        }
        coupons.createShopCoupon(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Offer saved.");
        return "redirect:/owner/offers";
    }

    @PostMapping("/owner/offers/{id}/deactivate")
    public String deactivate(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        coupons.deactivateOwn(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Offer deactivated.");
        return "redirect:/owner/offers";
    }
}
