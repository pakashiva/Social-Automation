package com.shoppingmall.repository;

import com.shoppingmall.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCartCustomerUserId(Long id, Long userId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    long countByCartId(Long cartId);
}
