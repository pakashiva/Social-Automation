package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OwnerDashboardController {

    private final DashboardService dashboards;

    public OwnerDashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/owner/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboards.owner(MallUserDetails.requireId(authentication)));
        return "owner/dashboard";
    }
}
