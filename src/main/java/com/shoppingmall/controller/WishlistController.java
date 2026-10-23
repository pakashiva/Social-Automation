package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WishlistController {

    private final WishlistService wishlists;

    public WishlistController(WishlistService wishlists) {
        this.wishlists = wishlists;
    }

    @GetMapping("/customer/wishlist")
    public String view(Authentication authentication, Model model) {
        model.addAttribute("items", wishlists.view(MallUserDetails.requireId(authentication)));
        return "customer/wishlist";
    }

    @PostMapping("/customer/wishlist/items")
    public String add(Authentication authentication, @RequestParam long productId) {
        wishlists.add(MallUserDetails.requireId(authentication), productId);
        return "redirect:/customer/wishlist";
    }

    @PostMapping("/customer/wishlist/items/{id}/delete")
    public String remove(Authentication authentication, @PathVariable long id) {
        wishlists.remove(MallUserDetails.requireId(authentication), id);
        return "redirect:/customer/wishlist";
    }

    @PostMapping("/customer/wishlist/items/{id}/move")
    public String move(Authentication authentication, @PathVariable long id) {
        wishlists.moveToCart(MallUserDetails.requireId(authentication), id);
        return "redirect:/customer/cart";
    }
}
