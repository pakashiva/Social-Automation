package com.shoppingmall.controller;

import com.shoppingmall.dto.ComplaintUpdateForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ComplaintService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminComplaintController {

    private final ComplaintService complaints;

    public AdminComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    @GetMapping("/admin/complaints")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("complaints", complaints.listAll(page));
        return "admin/complaints";
    }

    @GetMapping("/admin/complaints/{complaintNumber}")
    public String details(@PathVariable String complaintNumber, Model model) {
        model.addAttribute("complaint", complaints.get(complaintNumber));
        return "admin/complaint-details";
    }

    @PostMapping("/admin/complaints/{complaintNumber}/assign")
    public String assign(
            @PathVariable String complaintNumber,
            ComplaintUpdateForm form,
            RedirectAttributes redirectAttributes) {
        complaints.assign(complaintNumber, form.getAssigneeId());
        redirectAttributes.addFlashAttribute("success", "Complaint assigned.");
        return "redirect:/admin/complaints/" + complaintNumber;
    }

    @PostMapping("/admin/complaints/{complaintNumber}/resolve")
    public String resolve(
            Authentication authentication,
            @PathVariable String complaintNumber,
            ComplaintUpdateForm form,
            RedirectAttributes redirectAttributes) {
        complaints.resolve(MallUserDetails.requireId(authentication), complaintNumber, form.getResolutionNote());
        redirectAttributes.addFlashAttribute("success", "Complaint resolved.");
        return "redirect:/admin/complaints/" + complaintNumber;
    }

    @PostMapping("/admin/complaints/{complaintNumber}/reject")
    public String reject(
            Authentication authentication,
            @PathVariable String complaintNumber,
            ComplaintUpdateForm form,
            RedirectAttributes redirectAttributes) {
        complaints.reject(MallUserDetails.requireId(authentication), complaintNumber, form.getResolutionNote());
        redirectAttributes.addFlashAttribute("success", "Complaint rejected.");
        return "redirect:/admin/complaints/" + complaintNumber;
    }
}
