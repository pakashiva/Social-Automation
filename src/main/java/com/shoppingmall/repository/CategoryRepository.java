package com.shoppingmall.repository;

import com.shoppingmall.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();
    Page<Category> findAllByOrderByDisplayOrderAscNameAsc(Pageable pageable);
    long countByParentId(Long parentId);
}
