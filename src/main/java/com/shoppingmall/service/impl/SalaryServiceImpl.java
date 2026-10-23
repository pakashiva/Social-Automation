package com.shoppingmall.service.impl;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Attendance;
import com.shoppingmall.entity.Employee;
import com.shoppingmall.entity.SalaryRecord;
import com.shoppingmall.enums.AttendanceStatus;
import com.shoppingmall.enums.EmployeeStatus;
import com.shoppingmall.enums.SalaryStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.AttendanceRepository;
import com.shoppingmall.repository.EmployeeRepository;
import com.shoppingmall.repository.SalaryRecordRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.SalaryService;
import com.shoppingmall.util.Money;
import com.shoppingmall.util.Pages;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryServiceImpl implements SalaryService {

    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final SalaryRecordRepository salaries;
    private final OwnershipGuard guard;
    private final AuditService audit;
    private final Clock clock;

    public SalaryServiceImpl(
            EmployeeRepository employees,
            AttendanceRepository attendance,
            SalaryRecordRepository salaries,
            OwnershipGuard guard,
            AuditService audit,
            Clock clock) {
        this.employees = employees;
        this.attendance = attendance;
        this.salaries = salaries;
        this.guard = guard;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.SalaryRow> list(int page) {
        return salaries.findAllByOrderByYearMonthDescIdDesc(Pages.of(page)).map(this::row);
    }

    @Override
    @Transactional
    public String generate(long adminId, String yearMonth) {
        YearMonth month = parse(yearMonth);
        YearMonth current = YearMonth.now(clock);
        if (month.isAfter(current)) {
            throw new BusinessException("You cannot generate a future month.", "/admin/salaries");
        }
        int skipped = 0;
        for (Employee employee : employees.findByStatusOrderByFullNameAsc(EmployeeStatus.ACTIVE)) {
            if (salaries.findByEmployeeIdAndYearMonth(employee.getId(), month.toString()).isPresent()) {
                skipped++;
                continue;
            }
            List<Attendance> rows = attendance.findByEmployeeIdAndWorkDateBetween(
                    employee.getId(), month.atDay(1), month.atEndOfMonth());
            int present = count(rows, AttendanceStatus.PRESENT);
            int absent = count(rows, AttendanceStatus.ABSENT);
            int half = count(rows, AttendanceStatus.HALF_DAY);
            int leave = count(rows, AttendanceStatus.LEAVE);
            BigDecimal base = Money.scale(employee.getMonthlySalary());
            BigDecimal deduction = deduction(base, month.lengthOfMonth(), absent, half);
            SalaryRecord record = new SalaryRecord();
            record.setEmployee(employee);
            record.setYearMonth(month.toString());
            record.setBaseAmount(base);
            record.setDeductionAmount(deduction);
            record.setNetAmount(Money.scale(base.subtract(deduction)));
            record.setPresentDays(present);
            record.setAbsentDays(absent);
            record.setHalfDays(half);
            record.setLeaveDays(leave);
            record.setStatus(SalaryStatus.DRAFT);
            record.setGeneratedBy(guard.requireUser(adminId));
            salaries.save(record);
        }
        if (skipped > 0) {
            return "Skipped " + skipped + " employees who already have a salary row.";
        }
        return "Salary rows were generated.";
    }

    @Override
    @Transactional
    public void markPaid(long adminId, long id) {
        SalaryRecord record = salaries.findById(id).orElseThrow(() -> new NotFoundException("Salary was not found."));
        if (record.getStatus() == SalaryStatus.PAID) {
            throw new BusinessException("This salary is already marked paid.", "/admin/salaries");
        }
        record.setStatus(SalaryStatus.PAID);
        record.setPaidAt(LocalDateTime.now(clock));
        salaries.save(record);
        audit.record(adminId, "SALARY_PAID", "salary_records", record.getId(),
                record.getEmployee().getEmployeeCode() + " " + record.getYearMonth());
    }

    static BigDecimal deduction(BigDecimal monthly, int daysInMonth, int absentDays, int halfDays) {
        BigDecimal daily = monthly.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal half = daily.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal amount = daily.multiply(BigDecimal.valueOf(absentDays)).add(half.multiply(BigDecimal.valueOf(halfDays)));
        amount = Money.scale(amount);
        if (amount.compareTo(monthly) > 0) {
            return Money.scale(monthly);
        }
        return amount;
    }

    private static int count(List<Attendance> rows, AttendanceStatus status) {
        return (int) rows.stream().filter(row -> row.getStatus() == status).count();
    }

    private static YearMonth parse(String yearMonth) {
        if (yearMonth == null || !yearMonth.matches("[0-9]{4}-[0-9]{2}")) {
            throw new BusinessException("Enter a month as YYYY-MM.", "/admin/salaries");
        }
        return YearMonth.parse(yearMonth);
    }

    private Rows.SalaryRow row(SalaryRecord record) {
        Rows.SalaryRow row = new Rows.SalaryRow();
        row.setId(record.getId());
        row.setEmployeeName(record.getEmployee().getFullName());
        row.setYearMonth(record.getYearMonth());
        row.setBaseAmount(record.getBaseAmount());
        row.setDeductionAmount(record.getDeductionAmount());
        row.setNetAmount(record.getNetAmount());
        row.setPresentDays(record.getPresentDays());
        row.setAbsentDays(record.getAbsentDays());
        row.setHalfDays(record.getHalfDays());
        row.setLeaveDays(record.getLeaveDays());
        row.setStatus(record.getStatus().name());
        return row;
    }
}
