package com.shoppingmall.repository;

import com.shoppingmall.entity.Invoice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByOrderId(Long orderId);
    Page<Invoice> findByCustomerIdOrderByIssuedAtDesc(Long customerId, Pageable pageable);
    long countByOrderId(Long orderId);
}
