package com.shoppingmall.controller;

import com.shoppingmall.service.CategoryService;
import com.shoppingmall.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicProductController {

    private final ProductService products;
    private final CategoryService categories;

    public PublicProductController(ProductService products, CategoryService categories) {
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("products", products.searchPublic(q, category, page));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("category", category == null ? "" : category);
        return "public/products";
    }

    @GetMapping("/products/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("product", products.getPublic(id));
        return "public/product-details";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categories.listActive());
        return "public/categories";
    }

    @GetMapping("/categories/{slug}")
    public String byCategory(@PathVariable String slug, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", products.searchPublic("", slug, page));
        model.addAttribute("q", "");
        model.addAttribute("category", slug);
        return "public/products";
    }
}
