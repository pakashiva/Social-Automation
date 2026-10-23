package com.shoppingmall.controller;

import com.shoppingmall.dto.ShopProfileForm;
import com.shoppingmall.security.MallUserDetails;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OwnerShopController {

    private final ShopService shops;

    public OwnerShopController(ShopService shops) {
        this.shops = shops;
    }

    @GetMapping("/owner/shop-profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ShopProfileForm());
        }
        return "owner/shop-profile";
    }

    @PostMapping("/owner/shop-profile/{shopId}")
    public String update(
            Authentication authentication,
            @PathVariable long shopId,
            @Valid @ModelAttribute("form") ShopProfileForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
            model.addAttribute("editingShopId", shopId);
            return "owner/shop-profile";
        }
        shops.updateOwn(MallUserDetails.requireId(authentication), shopId, form);
        redirectAttributes.addFlashAttribute("success", "Shop profile saved.");
        return "redirect:/owner/shop-profile";
    }
}
