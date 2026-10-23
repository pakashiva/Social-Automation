package com.shoppingmall.service;

import com.shoppingmall.dto.Rows;
import com.shoppingmall.enums.UserStatus;
import java.util.List;
import org.springframework.data.domain.Page;

public interface UserAdminService {

    Page<Rows.UserRow> search(String query, int page);

    Rows.UserRow get(long id);

    void changeStatus(long adminId, long userId, UserStatus status);

    List<Rows.RoleCount> roleSummary();
}
