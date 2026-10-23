package com.shoppingmall.repository;

import com.shoppingmall.entity.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);

    @Query("""
            select c from Customer c join c.user u
            where :q = '' or lower(u.fullName) like :q or lower(u.email) like :q or u.mobile like :q
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
