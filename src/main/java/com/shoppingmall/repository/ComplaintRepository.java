package com.shoppingmall.repository;

import com.shoppingmall.entity.Complaint;
import com.shoppingmall.enums.ComplaintStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByComplaintNumber(String complaintNumber);
    Page<Complaint> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(ComplaintStatus status);
}
