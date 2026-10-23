package com.shoppingmall.controller;

import com.shoppingmall.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicShopController {

    private final ShopService shops;

    public PublicShopController(ShopService shops) {
        this.shops = shops;
    }

    @GetMapping("/shops")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("shops", shops.listPublic(page));
        return "public/shops";
    }

    @GetMapping("/shops/{slug}")
    public String details(@PathVariable String slug, Model model) {
        model.addAttribute("shop", shops.getPublicBySlug(slug));
        return "public/shop-details";
    }
}
