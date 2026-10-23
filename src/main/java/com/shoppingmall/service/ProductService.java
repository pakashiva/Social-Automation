package com.shoppingmall.service;

import com.shoppingmall.dto.ProductCard;
import com.shoppingmall.dto.ProductForm;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ProductService {

    List<ProductCard> featured();

    Page<ProductCard> searchPublic(String query, String categorySlug, int page);

    ProductCard getPublic(long id);

    Page<ProductCard> listForOwner(long userId, int page);

    Page<ProductCard> listAll(int page);

    ProductCard getForOwner(long userId, long id);

    long create(long userId, ProductForm form);

    void update(long userId, long id, ProductForm form);

    void archive(long adminId, long id);

    String imagePath(long productId);
}
