package com.shoppingmall.controller;

import com.shoppingmall.dto.FeedbackForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerFeedbackController {

    private final FeedbackService feedback;

    public CustomerFeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @GetMapping("/customer/feedback")
    public String page(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("feedback", feedback.listForCustomer(MallUserDetails.requireId(authentication), page));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new FeedbackForm());
        }
        return "customer/feedback";
    }

    @PostMapping("/customer/feedback")
    public String submit(
            Authentication authentication,
            @Valid @ModelAttribute("form") FeedbackForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("feedback", feedback.listForCustomer(MallUserDetails.requireId(authentication), 0));
            return "customer/feedback";
        }
        feedback.submitProfile(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Feedback saved.");
        return "redirect:/customer/feedback";
    }
}
