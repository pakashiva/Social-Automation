package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerDashboardController {

    private final DashboardService dashboards;

    public CustomerDashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/customer/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboards.customer(MallUserDetails.requireId(authentication)));
        return "customer/dashboard";
    }
}
