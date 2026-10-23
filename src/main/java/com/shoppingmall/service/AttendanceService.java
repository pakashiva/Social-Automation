package com.shoppingmall.service;

import com.shoppingmall.dto.AttendanceForm;
import com.shoppingmall.dto.Rows;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    List<Rows.AttendanceRow> forDate(LocalDate date);

    void save(long adminId, AttendanceForm form);
}
