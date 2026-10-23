package com.shoppingmall.service.impl;

import com.shoppingmall.dto.CategoryForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Category;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.CategoryRepository;
import com.shoppingmall.service.CategoryService;
import com.shoppingmall.util.Pages;
import com.shoppingmall.util.Slugger;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categories;

    public CategoryServiceImpl(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rows.CategoryRow> listActive() {
        return categories.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(this::row).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.CategoryRow> listAll(int page) {
        return categories.findAllByOrderByDisplayOrderAscNameAsc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional
    public void save(CategoryForm form) {
        Category category = form.getId() == null
                ? new Category()
                : categories.findById(form.getId()).orElseThrow(() -> new NotFoundException("Category was not found."));
        Category parent = null;
        if (form.getParentId() != null) {
            parent = categories.findById(form.getParentId())
                    .orElseThrow(() -> new NotFoundException("Category was not found."));
            if (category.getId() != null && category.getId().equals(parent.getId())) {
                throw new BusinessException("A category cannot be its own parent.", "/admin/categories");
            }
            if (parent.getParent() != null) {
                throw new BusinessException("A category can only sit under a root.", "/admin/categories");
            }
            if (category.getId() != null && categories.countByParentId(category.getId()) > 0) {
                throw new BusinessException("Move the child categories before this one can become a child.", "/admin/categories");
            }
        }
        if (category.getId() == null) {
            String base = Slugger.slug(form.getName());
            String slug = base;
            int suffix = 2;
            while (categories.existsBySlug(slug)) {
                slug = base + "-" + suffix;
                suffix++;
            }
            category.setSlug(slug);
        }
        category.setName(form.getName().trim());
        category.setDescription(form.getDescription());
        category.setParent(parent);
        category.setActive(form.isActive());
        category.setDisplayOrder(form.getDisplayOrder() == null ? 0 : form.getDisplayOrder());
        categories.save(category);
    }

    private Rows.CategoryRow row(Category category) {
        Rows.CategoryRow row = new Rows.CategoryRow();
        row.setId(category.getId());
        row.setName(category.getName());
        row.setSlug(category.getSlug());
        row.setDescription(category.getDescription());
        row.setParentId(category.getParent() == null ? null : category.getParent().getId());
        row.setParentName(category.getParent() == null ? "" : category.getParent().getName());
        row.setActive(category.isActive());
        row.setDisplayOrder(category.getDisplayOrder());
        return row;
    }
}
