package com.shoppingmall.service;

import com.shoppingmall.dto.SettingsForm;

public interface SettingsService {

    SettingsForm get();

    void update(long adminId, SettingsForm form);
}
