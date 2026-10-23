package com.shoppingmall.repository;

import com.shoppingmall.entity.Checkout;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutRepository extends JpaRepository<Checkout, Long> {
    Optional<Checkout> findByIdempotencyKey(String idempotencyKey);
}
