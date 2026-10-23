package com.shoppingmall.service;

import com.shoppingmall.dto.EmployeeForm;
import com.shoppingmall.dto.Rows;
import org.springframework.data.domain.Page;

public interface StaffService {

    Page<Rows.EmployeeRow> list(int page);

    Rows.EmployeeRow get(long id);

    void create(EmployeeForm form);

    void update(long id, EmployeeForm form);
}
