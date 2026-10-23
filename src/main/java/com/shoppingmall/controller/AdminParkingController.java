package com.shoppingmall.controller;

import com.shoppingmall.dto.ParkingEntryForm;
import com.shoppingmall.dto.ParkingRateForm;
import com.shoppingmall.dto.SlotStatusForm;
import com.shoppingmall.enums.VehicleType;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminParkingController {

    private final ParkingService parking;

    public AdminParkingController(ParkingService parking) {
        this.parking = parking;
    }

    @GetMapping("/admin/parking")
    public String board(Model model) {
        model.addAttribute("board", parking.board());
        return "admin/parking";
    }

    @PostMapping("/admin/parking/sessions")
    public String enter(
            Authentication authentication,
            @Valid @ModelAttribute ParkingEntryForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Check the vehicle and slot.");
            return "redirect:/admin/parking";
        }
        parking.enter(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Vehicle entered.");
        return "redirect:/admin/parking";
    }

    @PostMapping("/admin/parking/sessions/{ticketNumber}/exit")
    public String exit(@PathVariable String ticketNumber, RedirectAttributes redirectAttributes) {
        parking.exit(ticketNumber);
        redirectAttributes.addFlashAttribute("success", "Ticket closed.");
        return "redirect:/admin/parking";
    }

    @PostMapping("/admin/parking/sessions/{ticketNumber}/void")
    public String voidTicket(
            Authentication authentication,
            @PathVariable String ticketNumber,
            RedirectAttributes redirectAttributes) {
        parking.voidOpen(MallUserDetails.requireId(authentication), ticketNumber);
        redirectAttributes.addFlashAttribute("success", "Ticket voided.");
        return "redirect:/admin/parking";
    }

    @PostMapping("/admin/parking/rates/{vehicleType}")
    public String rate(
            Authentication authentication,
            @PathVariable VehicleType vehicleType,
            @Valid @ModelAttribute ParkingRateForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Check the rate card.");
            return "redirect:/admin/parking";
        }
        parking.updateRate(MallUserDetails.requireId(authentication), vehicleType, form);
        redirectAttributes.addFlashAttribute("success", "Rate updated.");
        return "redirect:/admin/parking";
    }

    @PostMapping("/admin/parking/slots/{id}/status")
    public String slot(
            @PathVariable long id,
            @Valid @ModelAttribute SlotStatusForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Choose a slot status.");
            return "redirect:/admin/parking";
        }
        parking.changeSlot(id, form.getStatus());
        redirectAttributes.addFlashAttribute("success", "Slot updated.");
        return "redirect:/admin/parking";
    }
}
