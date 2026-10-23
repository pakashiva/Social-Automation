package com.shoppingmall.repository;

import com.shoppingmall.entity.Employee;
import com.shoppingmall.enums.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByMobile(String mobile);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    Page<Employee> findAllByOrderByEmployeeCodeAsc(Pageable pageable);
    List<Employee> findByStatusOrderByFullNameAsc(EmployeeStatus status);
}
