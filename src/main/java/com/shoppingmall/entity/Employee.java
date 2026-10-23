package com.shoppingmall.entity;

import com.shoppingmall.enums.EmployeeDepartment;
import com.shoppingmall.enums.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employees")
public class Employee extends AuditedEntity {

    @Column(name = "employee_code", nullable = false, length = 20)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mobile", length = 10)
    private String mobile;

    @Column(name = "job_title", nullable = false, length = 80)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 40)
    private EmployeeDepartment department;

    @Column(name = "hired_on", nullable = false)
    private LocalDate hiredOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "monthly_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlySalary;
}
