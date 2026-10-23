package com.shoppingmall.repository;

import com.shoppingmall.entity.SalaryRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    Optional<SalaryRecord> findByEmployeeIdAndYearMonth(Long employeeId, String yearMonth);
    Page<SalaryRecord> findAllByOrderByYearMonthDescIdDesc(Pageable pageable);
}
