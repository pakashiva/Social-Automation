package com.shoppingmall.controller;

import com.shoppingmall.dto.ForgotPasswordForm;
import com.shoppingmall.dto.ResetPasswordForm;
import com.shoppingmall.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetController {

    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    @GetMapping("/forgot-password")
    public String forgot(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ForgotPasswordForm());
        }
        return "public/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String issue(@Valid @ModelAttribute("form") ForgotPasswordForm form, BindingResult binding) {
        if (binding.hasErrors()) {
            return "public/forgot-password";
        }
        resets.issue(form.getEmail());
        return "redirect:/forgot-password?sent";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("form", form);
        model.addAttribute("tokenValid", resets.peek(token));
        return "public/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(
            @Valid @ModelAttribute("form") ResetPasswordForm form,
            BindingResult binding,
            HttpServletRequest request,
            Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("tokenValid", resets.peek(form.getToken()));
            return "public/reset-password";
        }
        resets.reset(form.getToken(), form.getPassword(), form.getConfirmPassword());
        request.getSession().invalidate();
        return "redirect:/login?reset";
    }
}
