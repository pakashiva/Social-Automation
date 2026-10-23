package com.shoppingmall.controller;

import com.shoppingmall.service.UserAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminRoleController {

    private final UserAdminService users;

    public AdminRoleController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping("/admin/roles")
    public String roles(Model model) {
        model.addAttribute("roles", users.roleSummary());
        return "admin/roles";
    }
}
