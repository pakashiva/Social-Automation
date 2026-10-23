package com.shoppingmall.controller;

import com.shoppingmall.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final DashboardService dashboards;

    public AdminDashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboards.admin());
        return "admin/dashboard";
    }
}
