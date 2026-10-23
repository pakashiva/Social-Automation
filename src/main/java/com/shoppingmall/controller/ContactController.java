package com.shoppingmall.controller;

import com.shoppingmall.dto.ContactForm;
import com.shoppingmall.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    private final FeedbackService feedback;

    public ContactController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @GetMapping("/contact")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ContactForm());
        }
        return "public/contact";
    }

    @PostMapping("/contact")
    public String submit(
            @Valid @ModelAttribute("form") ContactForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return "public/contact";
        }
        feedback.submitContact(form);
        redirectAttributes.addFlashAttribute("success", "Your message was sent.");
        return "redirect:/contact";
    }
}
