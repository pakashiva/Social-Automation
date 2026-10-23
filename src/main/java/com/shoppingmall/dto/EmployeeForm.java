package com.shoppingmall.dto;


import com.shoppingmall.enums.EmployeeDepartment;
import com.shoppingmall.enums.EmployeeStatus;
import com.shoppingmall.validation.MobileNumber;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmployeeForm {
    @NotBlank @Size(max = 120)
    private String fullName;
    @Email
    private String email;
    @MobileNumber
    private String mobile;
    @NotBlank @Size(max = 80)
    private String jobTitle;
    @NotNull
    private EmployeeDepartment department;
    @NotNull
    private LocalDate hiredOn;
    @NotNull @DecimalMin("0.00")
    private BigDecimal monthlySalary;
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
}
