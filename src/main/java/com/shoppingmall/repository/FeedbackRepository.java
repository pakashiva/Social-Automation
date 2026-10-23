package com.shoppingmall.repository;

import com.shoppingmall.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
