package com.shoppingmall.controller;

import com.shoppingmall.dto.StockAdjustForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.InventoryService;
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
public class OwnerInventoryController {

    private final InventoryService inventory;

    public OwnerInventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/owner/inventory")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("rows", inventory.listForOwner(MallUserDetails.requireId(authentication), page));
        return "owner/inventory";
    }

    @PostMapping("/owner/inventory/{productId}/adjust")
    public String adjust(
            Authentication authentication,
            @PathVariable long productId,
            @Valid @ModelAttribute StockAdjustForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Check the stock adjustment and try again.");
            return "redirect:/owner/inventory";
        }
        inventory.adjust(MallUserDetails.requireId(authentication), productId, form);
        redirectAttributes.addFlashAttribute("success", "Stock updated.");
        return "redirect:/owner/inventory";
    }
}
