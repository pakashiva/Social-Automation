package com.shoppingmall.controller;

import com.shoppingmall.dto.CategoryForm;
import com.shoppingmall.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCategoryController {

    private final CategoryService categories;

    public AdminCategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping("/admin/categories")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("categories", categories.listAll(page));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CategoryForm());
        }
        return "admin/categories";
    }

    @PostMapping({"/admin/categories", "/admin/categories/{id}"})
    public String save(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") CategoryForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (id != null) {
            form.setId(id);
        }
        if (binding.hasErrors()) {
            model.addAttribute("categories", categories.listAll(0));
            return "admin/categories";
        }
        categories.save(form);
        redirectAttributes.addFlashAttribute("success", "Category saved.");
        return "redirect:/admin/categories";
    }
}
