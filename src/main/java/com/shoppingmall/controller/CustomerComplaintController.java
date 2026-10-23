package com.shoppingmall.controller;

import com.shoppingmall.dto.ComplaintForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ComplaintService;
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
public class CustomerComplaintController {

    private final ComplaintService complaints;

    public CustomerComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    @GetMapping("/customer/complaints")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("complaints", complaints.listForCustomer(MallUserDetails.requireId(authentication), page));
        return "customer/complaints";
    }

    @GetMapping("/customer/complaints/new")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ComplaintForm());
        }
        return "customer/complaint-form";
    }

    @PostMapping("/customer/complaints")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("form") ComplaintForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return "customer/complaint-form";
        }
        complaints.create(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Complaint submitted.");
        return "redirect:/customer/complaints";
    }

    @GetMapping("/customer/complaints/{complaintNumber}")
    public String details(Authentication authentication, @PathVariable String complaintNumber, Model model) {
        model.addAttribute("complaint", complaints.getForCustomer(MallUserDetails.requireId(authentication), complaintNumber));
        return "customer/complaint-details";
    }
}
