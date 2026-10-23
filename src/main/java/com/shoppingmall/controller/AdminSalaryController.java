package com.shoppingmall.controller;

import com.shoppingmall.dto.SalaryGenerateForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.SalaryService;
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
public class AdminSalaryController {

    private final SalaryService salaries;

    public AdminSalaryController(SalaryService salaries) {
        this.salaries = salaries;
    }

    @GetMapping("/admin/salaries")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("salaries", salaries.list(page));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new SalaryGenerateForm());
        }
        return "admin/salaries";
    }

    @PostMapping("/admin/salaries/generate")
    public String generate(
            Authentication authentication,
            @Valid @ModelAttribute("form") SalaryGenerateForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("salaries", salaries.list(0));
            return "admin/salaries";
        }
        redirectAttributes.addFlashAttribute("success", salaries.generate(MallUserDetails.requireId(authentication), form.getYearMonth()));
        return "redirect:/admin/salaries";
    }

    @PostMapping("/admin/salaries/{id}/pay")
    public String pay(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes) {
        salaries.markPaid(MallUserDetails.requireId(authentication), id);
        redirectAttributes.addFlashAttribute("success", "Salary marked paid.");
        return "redirect:/admin/salaries";
    }
}
