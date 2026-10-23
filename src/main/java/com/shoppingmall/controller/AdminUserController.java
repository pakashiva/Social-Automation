package com.shoppingmall.controller;

import com.shoppingmall.dto.UserStatusForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.UserAdminService;
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
public class AdminUserController {

    private final UserAdminService users;

    public AdminUserController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping("/admin/users")
    public String list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", users.search(q, page));
        model.addAttribute("q", q == null ? "" : q);
        return "admin/users";
    }

    @GetMapping("/admin/users/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("user", users.get(id));
        return "admin/user-details";
    }

    @PostMapping("/admin/users/{id}/status")
    public String status(
            Authentication authentication,
            @PathVariable long id,
            @Valid @ModelAttribute UserStatusForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Choose a status.");
            return "redirect:/admin/users/" + id;
        }
        users.changeStatus(MallUserDetails.requireId(authentication), id, form.getStatus());
        redirectAttributes.addFlashAttribute("success", "User status updated.");
        return "redirect:/admin/users/" + id;
    }
}
