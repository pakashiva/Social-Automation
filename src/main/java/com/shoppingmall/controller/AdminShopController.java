package com.shoppingmall.controller;

import com.shoppingmall.dto.ShopDecisionForm;
import com.shoppingmall.enums.ShopStatus;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ShopService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminShopController {

    private final ShopService shops;

    public AdminShopController(ShopService shops) {
        this.shops = shops;
    }

    @GetMapping("/admin/shops")
    public String list(
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("shops", shops.listAll(page, status));
        model.addAttribute("status", status == null ? "" : status.name());
        return "admin/shops";
    }

    @GetMapping("/admin/shops/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("shop", shops.get(id));
        return "admin/shop-review";
    }

    @PostMapping("/admin/shops/{id}/approve")
    public String approve(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        shops.approve(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Shop approved.");
        return "redirect:/admin/shops/" + id;
    }

    @PostMapping("/admin/shops/{id}/reject")
    public String reject(
            Authentication authentication,
            @PathVariable long id,
            ShopDecisionForm form,
            RedirectAttributes redirectAttributes) {
        shops.reject(MallUserDetails.requireId(authentication), id, form.getRejectionReason());
        redirectAttributes.addFlashAttribute("success", "Shop rejected.");
        return "redirect:/admin/shops/" + id;
    }

    @PostMapping("/admin/shops/{id}/status")
    public String status(
            Authentication authentication,
            @PathVariable long id,
            ShopDecisionForm form,
            RedirectAttributes redirectAttributes) {
        shops.changeStatus(MallUserDetails.requireId(authentication), id, form.getStatus());
        redirectAttributes.addFlashAttribute("success", "Shop status updated.");
        return "redirect:/admin/shops/" + id;
    }
}
