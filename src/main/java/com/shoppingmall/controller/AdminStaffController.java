package com.shoppingmall.controller;

import com.shoppingmall.dto.EmployeeForm;
import com.shoppingmall.exception.DuplicateAccountException;
import com.shoppingmall.service.StaffService;
import jakarta.validation.Valid;
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
public class AdminStaffController {

    private final StaffService staff;

    public AdminStaffController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping("/admin/staff")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("employees", staff.list(page));
        return "admin/staff";
    }

    @GetMapping("/admin/staff/new")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new EmployeeForm());
        }
        return "admin/add-employee";
    }

    @PostMapping("/admin/staff")
    public String create(
            @Valid @ModelAttribute("form") EmployeeForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return "admin/add-employee";
        }
        try {
            staff.create(form);
        } catch (DuplicateAccountException exception) {
            binding.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            return "admin/add-employee";
        }
        redirectAttributes.addFlashAttribute("success", "Employee saved.");
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") EmployeeForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("employees", staff.list(0));
            model.addAttribute("editingId", id);
            return "admin/staff";
        }
        try {
            staff.update(id, form);
        } catch (DuplicateAccountException exception) {
            binding.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("employees", staff.list(0));
            model.addAttribute("editingId", id);
            return "admin/staff";
        }
        redirectAttributes.addFlashAttribute("success", "Employee saved.");
        return "redirect:/admin/staff";
    }
}
