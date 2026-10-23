package com.shoppingmall.controller;

import com.shoppingmall.dto.ReviewForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping("/customer/reviews")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute ReviewForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Check the review and try again.");
            return "redirect:/customer/orders";
        }
        reviews.create(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Review published.");
        return "redirect:/customer/orders";
    }
}
