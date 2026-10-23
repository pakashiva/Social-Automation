package com.shoppingmall.repository;

import com.shoppingmall.entity.WishlistItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByIdAndWishlistCustomerUserId(Long id, Long userId);
    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);
    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);
    long countByWishlistId(Long wishlistId);
}
