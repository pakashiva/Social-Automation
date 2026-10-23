package com.shoppingmall.controller;

import com.shoppingmall.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAuditController {

    private final AuditService audit;

    public AdminAuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/admin/audit-logs")
    public String list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actor,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("logs", audit.search(action, actor, page));
        model.addAttribute("action", action == null ? "" : action);
        model.addAttribute("actor", actor);
        model.addAttribute("filtered", (action != null && !action.isBlank()) || actor != null);
        return "admin/audit-logs";
    }
}
