package com.shoppingmall.controller;

import com.shoppingmall.enums.FeedbackStatus;
import com.shoppingmall.service.FeedbackService;
import com.shoppingmall.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCustomerController {

    private final ProfileService profiles;
    private final FeedbackService feedback;

    public AdminCustomerController(ProfileService profiles, FeedbackService feedback) {
        this.profiles = profiles;
        this.feedback = feedback;
    }

    @GetMapping("/admin/customers")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("customers", profiles.searchCustomers(q, page));
        model.addAttribute("feedback", feedback.listAll(page));
        model.addAttribute("q", q == null ? "" : q);
        return "admin/customers";
    }

    @PostMapping("/admin/feedback/{id}/status")
    public String status(@PathVariable long id, @RequestParam FeedbackStatus status, RedirectAttributes redirectAttributes) {
        feedback.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "Feedback status updated.");
        return "redirect:/admin/customers";
    }
}
