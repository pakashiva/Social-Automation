package com.shoppingmall.controller;

import com.shoppingmall.dto.ProductForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.CategoryService;
import com.shoppingmall.service.ProductService;
import com.shoppingmall.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
public class OwnerProductController {

    private final ProductService products;
    private final CategoryService categories;
    private final ShopService shops;

    public OwnerProductController(ProductService products, CategoryService categories, ShopService shops) {
        this.products = products;
        this.categories = categories;
        this.shops = shops;
    }

    @GetMapping("/owner/products")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", products.listForOwner(MallUserDetails.requireId(authentication), page));
        return "owner/products";
    }

    @GetMapping("/owner/products/new")
    public String createForm(Authentication authentication, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProductForm());
        }
        model.addAttribute("categories", categories.listActive());
        model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
        return "owner/add-product";
    }

    @PostMapping("/owner/products")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("form") ProductForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("categories", categories.listActive());
            model.addAttribute("shops", shops.listForOwner(MallUserDetails.requireId(authentication)));
            return "owner/add-product";
        }
        long id = products.create(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Product saved.");
        return "redirect:/owner/products/" + id + "/edit";
    }

    @GetMapping("/owner/products/{id}/edit")
    public String editForm(Authentication authentication, @PathVariable long id, Model model) {
        var product = products.getForOwner(MallUserDetails.requireId(authentication), id);
        if (!model.containsAttribute("form")) {
            ProductForm form = new ProductForm();
            form.setShopId(product.getShopId());
            form.setCategoryId(product.getCategoryId());
            form.setName(product.getName());
            form.setSku(product.getSku());
            form.setDescription(product.getDescription());
            form.setUnitPrice(product.getUnitPrice());
            form.setCompareAtPrice(product.getCompareAtPrice());
            form.setTaxRatePercent(product.getTaxRatePercent());
            form.setStatus(com.shoppingmall.enums.ProductStatus.valueOf(product.getStatus()));
            model.addAttribute("form", form);
        }
        model.addAttribute("product", product);
        model.addAttribute("categories", categories.listActive());
        return "owner/edit-product";
    }

    @PostMapping("/owner/products/{id}")
    public String update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @ModelAttribute("form") ProductForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("product", products.getForOwner(MallUserDetails.requireId(authentication), id));
            model.addAttribute("categories", categories.listActive());
            return "owner/edit-product";
        }
        products.update(MallUserDetails.requireId(authentication), id, form);
        redirectAttributes.addFlashAttribute("success", "Product saved.");
        return "redirect:/owner/products/" + id + "/edit";
    }
}
