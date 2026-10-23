package com.shoppingmall.repository;

import com.shoppingmall.entity.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByOrderItemId(Long orderItemId);
    boolean existsByOrderItemId(Long orderItemId);
}
