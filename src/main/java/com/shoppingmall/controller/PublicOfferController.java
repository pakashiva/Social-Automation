package com.shoppingmall.controller;

import com.shoppingmall.service.CouponService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicOfferController {

    private final CouponService coupons;

    public PublicOfferController(CouponService coupons) {
        this.coupons = coupons;
    }

    @GetMapping("/offers")
    public String offers(Model model) {
        model.addAttribute("offers", coupons.listPublic());
        return "public/offers";
    }
}
