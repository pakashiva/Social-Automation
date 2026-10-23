package com.shoppingmall.service.impl;

import com.shoppingmall.dto.AttendanceForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Attendance;
import com.shoppingmall.entity.Employee;
import com.shoppingmall.enums.AttendanceStatus;
import com.shoppingmall.enums.EmployeeStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.repository.AttendanceRepository;
import com.shoppingmall.repository.EmployeeRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AttendanceService;
import com.shoppingmall.util.TimeDisplay;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final OwnershipGuard guard;

    public AttendanceServiceImpl(
            EmployeeRepository employees, AttendanceRepository attendance, OwnershipGuard guard) {
        this.employees = employees;
        this.attendance = attendance;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rows.AttendanceRow> forDate(LocalDate date) {
        LocalDate workDate = date == null ? LocalDate.now(TimeDisplay.ZONE) : date;
        Map<Long, Attendance> byEmployee = attendance.findByWorkDate(workDate).stream()
                .collect(Collectors.toMap(row -> row.getEmployee().getId(), Function.identity()));
        List<Rows.AttendanceRow> rows = new ArrayList<>();
        for (Employee employee : employees.findByStatusOrderByFullNameAsc(EmployeeStatus.ACTIVE)) {
            Attendance saved = byEmployee.get(employee.getId());
            Rows.AttendanceRow row = new Rows.AttendanceRow();
            row.setEmployeeId(employee.getId());
            row.setEmployeeName(employee.getFullName());
            if (saved != null) {
                row.setStatus(saved.getStatus().name());
                row.setCheckIn(saved.getCheckIn());
                row.setCheckOut(saved.getCheckOut());
                row.setNote(saved.getNote());
            }
            rows.add(row);
        }
        return rows;
    }

    @Override
    @Transactional
    public void save(long adminId, AttendanceForm form) {
        Employee employee = employees.findById(form.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee was not found."));
        if (form.getStatus() == null || form.getWorkDate() == null) {
            throw new BusinessException("Choose a status and a date.", "/admin/attendance");
        }
        Attendance row = attendance.findByEmployeeIdAndWorkDate(employee.getId(), form.getWorkDate()).orElseGet(Attendance::new);
        row.setEmployee(employee);
        row.setWorkDate(form.getWorkDate());
        row.setStatus(form.getStatus());
        row.setRecordedBy(guard.requireUser(adminId));
        row.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        if (form.getStatus() == AttendanceStatus.ABSENT || form.getStatus() == AttendanceStatus.LEAVE) {
            row.setCheckIn(null);
            row.setCheckOut(null);
        } else {
            if (form.getCheckIn() == null) {
                throw new BusinessException("Check-in is required for this status.", "/admin/attendance");
            }
            if (form.getCheckOut() != null && !form.getCheckOut().isAfter(form.getCheckIn())) {
                throw new BusinessException("Check-out must be after check-in.", "/admin/attendance");
            }
            row.setCheckIn(form.getCheckIn());
            row.setCheckOut(form.getCheckOut());
        }
        attendance.save(row);
    }
}
