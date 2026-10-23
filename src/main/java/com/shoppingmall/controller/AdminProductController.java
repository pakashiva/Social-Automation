package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ProductService;
import com.shoppingmall.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminProductController {

    private final ProductService products;
    private final ReviewService reviews;

    public AdminProductController(ProductService products, ReviewService reviews) {
        this.products = products;
        this.reviews = reviews;
    }

    @GetMapping("/admin/products")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", products.listAll(page));
        return "admin/products";
    }

    @PostMapping("/admin/products/{id}/archive")
    public String archive(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        products.archive(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Product archived.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/reviews/{id}/hide")
    public String hide(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        reviews.hide(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Review hidden.");
        return "redirect:/admin/products";
    }
}
