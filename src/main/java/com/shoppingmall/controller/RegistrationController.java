package com.shoppingmall.controller;

import com.shoppingmall.dto.RegistrationForm;
import com.shoppingmall.exception.DuplicateAccountException;
import com.shoppingmall.security.LoginSuccessHandler;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final RegistrationService registration;

    public RegistrationController(RegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping("/register")
    public String form(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof MallUserDetails details) {
            return "redirect:" + LoginSuccessHandler.home(details.getRole());
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        return "public/register";
    }

    @PostMapping("/register")
    public String register(
            Authentication authentication,
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult binding,
            Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof MallUserDetails details) {
            return "redirect:" + LoginSuccessHandler.home(details.getRole());
        }
        if (binding.hasErrors()) {
            return "public/register";
        }
        try {
            registration.register(form);
        } catch (DuplicateAccountException exception) {
            binding.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            return "public/register";
        }
        if ("SHOP_OWNER".equals(form.getAccountType())) {
            return "redirect:/login?pending";
        }
        return "redirect:/login?registered";
    }
}
