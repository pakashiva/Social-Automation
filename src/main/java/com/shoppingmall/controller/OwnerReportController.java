package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ReportService;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OwnerReportController {

    private final ReportService reports;

    public OwnerReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/owner/reports")
    public String page(
            Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String orderStatus,
            Model model) {
        model.addAttribute("report", reports.ownerReport(MallUserDetails.requireId(authentication), from, to, orderStatus));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("orderStatus", orderStatus == null ? "" : orderStatus);
        return "owner/reports";
    }

    @GetMapping("/owner/reports/sales.json")
    @ResponseBody
    public Object chart(Authentication authentication) {
        return reports.ownerChart(MallUserDetails.requireId(authentication));
    }
}
