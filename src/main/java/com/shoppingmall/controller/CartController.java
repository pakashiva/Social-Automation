package com.shoppingmall.controller;

import com.shoppingmall.dto.CartItemForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.CartService;
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

@Controller
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping("/customer/cart")
    public String cart(Authentication authentication, Model model) {
        model.addAttribute("cart", carts.view(MallUserDetails.requireId(authentication)));
        return "customer/cart";
    }

    @PostMapping("/customer/cart/items")
    public String add(
            Authentication authentication,
            @Valid @ModelAttribute CartItemForm form,
            BindingResult binding) {
        if (binding.hasErrors()) {
            return "redirect:/customer/cart";
        }
        carts.add(MallUserDetails.requireId(authentication), form.getProductId(), form.getQuantity());
        return "redirect:/customer/cart";
    }

    @PostMapping("/customer/cart/items/{id}")
    public String update(
            Authentication authentication,
            @PathVariable long id,
            @RequestParam int quantity) {
        carts.updateQuantity(MallUserDetails.requireId(authentication), id, quantity);
        return "redirect:/customer/cart";
    }

    @PostMapping("/customer/cart/items/{id}/delete")
    public String remove(Authentication authentication, @PathVariable long id) {
        carts.remove(MallUserDetails.requireId(authentication), id);
        return "redirect:/customer/cart";
    }
}
