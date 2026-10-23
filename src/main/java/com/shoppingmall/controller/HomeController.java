package com.shoppingmall.controller;

import com.shoppingmall.service.ProductService;
import com.shoppingmall.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ShopService shops;
    private final ProductService products;

    public HomeController(ShopService shops, ProductService products) {
        this.shops = shops;
        this.products = products;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("shops", shops.featured());
        model.addAttribute("products", products.featured());
        return "public/index";
    }
}
