package com.shoppingmall.service;

import com.shoppingmall.dto.PasswordChangeForm;
import com.shoppingmall.dto.ProfileForm;
import com.shoppingmall.dto.Rows;
import org.springframework.data.domain.Page;

public interface ProfileService {

    ProfileForm get(long userId);

    void update(long userId, ProfileForm form);

    void changePassword(long userId, PasswordChangeForm form);

    Page<Rows.UserRow> searchCustomers(String query, int page);
}
