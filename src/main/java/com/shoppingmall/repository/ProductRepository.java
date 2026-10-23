package com.shoppingmall.repository;

import com.shoppingmall.entity.Product;
import com.shoppingmall.enums.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByShopIdAndSkuIgnoreCase(Long shopId, String sku);
    boolean existsByShopIdAndSlug(Long shopId, String slug);
    Optional<Product> findByShopIdAndSlug(Long shopId, String slug);
    Page<Product> findByShopIdIn(List<Long> shopIds, Pageable pageable);
    List<Product> findByShopId(Long shopId);
    long countByShopIdInAndStatus(List<Long> shopIds, ProductStatus status);

    @Query("""
            select p from Product p
            where p.status = com.shoppingmall.enums.ProductStatus.ACTIVE
              and p.shop.status = com.shoppingmall.enums.ShopStatus.ACTIVE
              and (:q = '' or lower(p.name) like :q or lower(p.sku) like :q)
              and (:categorySlug = '' or p.category.slug = :categorySlug)
            """)
    Page<Product> searchPublic(@Param("q") String q, @Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("""
            select p from Product p
            where p.status = com.shoppingmall.enums.ProductStatus.ACTIVE
              and p.shop.status = com.shoppingmall.enums.ShopStatus.ACTIVE
            order by p.id desc
            """)
    List<Product> featured(Pageable pageable);
}
