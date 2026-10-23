package com.shoppingmall.repository;

import com.shoppingmall.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
}
