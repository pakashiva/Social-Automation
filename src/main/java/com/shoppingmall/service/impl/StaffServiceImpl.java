package com.shoppingmall.service.impl;

import com.shoppingmall.dto.EmployeeForm;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Employee;
import com.shoppingmall.enums.EmployeeStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.DuplicateAccountException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.EmployeeRepository;
import com.shoppingmall.service.NumberSequenceService;
import com.shoppingmall.service.StaffService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.Pages;
import com.shoppingmall.validation.MobileNumber;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffServiceImpl implements StaffService {

    private final EmployeeRepository employees;
    private final NumberSequenceService sequences;

    public StaffServiceImpl(EmployeeRepository employees, NumberSequenceService sequences) {
        this.employees = employees;
        this.sequences = sequences;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.EmployeeRow> list(int page) {
        return employees.findAllByOrderByEmployeeCodeAsc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional(readOnly = true)
    public Rows.EmployeeRow get(long id) {
        return row(employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found.")));
    }

    @Override
    @Transactional
    public void create(EmployeeForm form) {
        Employee employee = new Employee();
        employee.setEmployeeCode(sequences.next("EMPLOYEE"));
        employee.setStatus(EmployeeStatus.ACTIVE);
        apply(employee, form, null);
        employees.save(employee);
    }

    @Override
    @Transactional
    public void update(long id, EmployeeForm form) {
        Employee employee = employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found."));
        apply(employee, form, employee.getId());
        if (form.getStatus() != null) {
            employee.setStatus(form.getStatus());
        }
        employees.save(employee);
    }

    private void apply(Employee employee, EmployeeForm form, Long currentId) {
        if (form.getMonthlySalary() == null || form.getMonthlySalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Monthly salary cannot be negative.", "/admin/staff/new");
        }
        String email = blank(form.getEmail()) ? null : form.getEmail().trim().toLowerCase();
        String mobile = blank(form.getMobile()) ? null : form.getMobile().trim();
        if (mobile != null && !FieldRules.mobile(mobile)) {
            throw new DuplicateAccountException("mobile", "Enter a 10-digit mobile number starting with 6, 7, 8, or 9.");
        }
        if (email != null) {
            employees.findByEmail(email).filter(found -> currentId == null || !found.getId().equals(currentId))
                    .ifPresent(found -> {
                        throw new DuplicateAccountException("email", "An account with this email already exists.");
                    });
        }
        if (mobile != null) {
            employees.findByMobile(mobile).filter(found -> currentId == null || !found.getId().equals(currentId))
                    .ifPresent(found -> {
                        throw new DuplicateAccountException("mobile", "An account with this mobile number already exists.");
                    });
        }
        employee.setFullName(form.getFullName().trim());
        employee.setEmail(email);
        employee.setMobile(mobile);
        employee.setJobTitle(form.getJobTitle().trim());
        employee.setDepartment(form.getDepartment());
        employee.setHiredOn(form.getHiredOn());
        employee.setMonthlySalary(Money.scale(form.getMonthlySalary()));
    }

    private Rows.EmployeeRow row(Employee employee) {
        Rows.EmployeeRow row = new Rows.EmployeeRow();
        row.setId(employee.getId());
        row.setCode(employee.getEmployeeCode());
        row.setFullName(employee.getFullName());
        row.setEmail(employee.getEmail());
        row.setMobile(employee.getMobile());
        row.setJobTitle(employee.getJobTitle());
        row.setDepartment(employee.getDepartment().name());
        row.setHiredOn(employee.getHiredOn());
        row.setStatus(employee.getStatus().name());
        row.setMonthlySalary(employee.getMonthlySalary());
        return row;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
