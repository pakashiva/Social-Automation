package com.shoppingmall.controller;

import com.shoppingmall.dto.PasswordChangeForm;
import com.shoppingmall.dto.ProfileForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerProfileController {

    private final ProfileService profiles;

    public CustomerProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/customer/profile")
    public String profile(Authentication authentication, Model model) {
        long userId = MallUserDetails.requireId(authentication);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", profiles.get(userId));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "customer/profile";
    }

    @PostMapping("/customer/profile")
    public String update(
            Authentication authentication,
            @Valid @ModelAttribute("form") ProfileForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        long userId = MallUserDetails.requireId(authentication);
        if (binding.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "customer/profile";
        }
        profiles.update(userId, form);
        redirectAttributes.addFlashAttribute("success", "Profile saved.");
        return "redirect:/customer/profile";
    }

    @PostMapping("/customer/profile/password")
    public String password(
            Authentication authentication,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult binding,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        long userId = MallUserDetails.requireId(authentication);
        if (binding.hasErrors()) {
            model.addAttribute("form", profiles.get(userId));
            return "customer/profile";
        }
        profiles.changePassword(userId, form);
        request.changeSessionId();
        redirectAttributes.addFlashAttribute("success", "Password changed.");
        return "redirect:/customer/profile";
    }
}
