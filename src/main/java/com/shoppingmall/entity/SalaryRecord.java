package com.shoppingmall.entity;

import com.shoppingmall.enums.SalaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "salary_records")
public class SalaryRecord extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "deduction_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "present_days", nullable = false)
    private int presentDays;

    @Column(name = "absent_days", nullable = false)
    private int absentDays;

    @Column(name = "half_days", nullable = false)
    private int halfDays;

    @Column(name = "leave_days", nullable = false)
    private int leaveDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SalaryStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;
}
