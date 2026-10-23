package com.shoppingmall.controller;

import com.shoppingmall.service.ReportService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminReportController {

    private final ReportService reports;

    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/admin/reports")
    public String page(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String orderStatus,
            Model model) {
        model.addAttribute("report", reports.adminReport(from, to, shopId, orderStatus));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("shopId", shopId);
        model.addAttribute("orderStatus", orderStatus == null ? "" : orderStatus);
        return "admin/reports";
    }

    @GetMapping("/admin/reports/sales.json")
    @ResponseBody
    public Object chart() {
        return reports.adminChart();
    }
}
