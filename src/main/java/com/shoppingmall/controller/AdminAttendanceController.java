package com.shoppingmall.controller;

import com.shoppingmall.dto.AttendanceForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminAttendanceController {

    private final AttendanceService attendance;

    public AdminAttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping("/admin/attendance")
    public String page(@RequestParam(required = false) LocalDate date, Model model) {
        model.addAttribute("rows", attendance.forDate(date));
        model.addAttribute("date", date);
        return "admin/attendance";
    }

    @PostMapping("/admin/attendance")
    public String save(
            Authentication authentication,
            @Valid @ModelAttribute AttendanceForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Check the attendance row and try again.");
            return "redirect:/admin/attendance";
        }
        attendance.save(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Attendance saved.");
        return "redirect:/admin/attendance?date=" + form.getWorkDate();
    }
}
