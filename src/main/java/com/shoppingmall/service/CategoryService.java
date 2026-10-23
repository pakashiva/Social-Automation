package com.shoppingmall.service;

import com.shoppingmall.dto.CategoryForm;
import com.shoppingmall.dto.Rows;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CategoryService {

    List<Rows.CategoryRow> listActive();

    Page<Rows.CategoryRow> listAll(int page);

    void save(CategoryForm form);
}
