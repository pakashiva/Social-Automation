package com.shoppingmall.service.impl;

import com.shoppingmall.dto.SettingsForm;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.entity.AppSetting;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.AppSettingRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.AuditService;
import com.shoppingmall.service.SettingsService;
import com.shoppingmall.util.Money;
import com.shoppingmall.validation.MobileNumber;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsServiceImpl implements SettingsService {

    private final AppSettingRepository settings;
    private final OwnershipGuard guard;
    private final AuditService audit;

    public SettingsServiceImpl(AppSettingRepository settings, OwnershipGuard guard, AuditService audit) {
        this.settings = settings;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsForm get() {
        AppSetting setting = settings.findById(1L).orElseThrow();
        SettingsForm form = new SettingsForm();
        form.setMallName(setting.getMallName());
        form.setDefaultTaxPercent(setting.getDefaultTaxPercent());
        form.setLowStockThreshold(setting.getLowStockThreshold());
        form.setMaxUploadBytes(setting.getMaxUploadBytes());
        form.setSupportEmail(setting.getSupportEmail());
        form.setSupportPhone(setting.getSupportPhone());
        return form;
    }

    @Override
    @Transactional
    public void update(long adminId, SettingsForm form) {
        if (form.getMallName() == null || form.getMallName().isBlank()) {
            throw new BusinessException("Enter the mall name.", "/admin/settings");
        }
        if (form.getDefaultTaxPercent() == null
                || form.getDefaultTaxPercent().compareTo(BigDecimal.ZERO) < 0
                || form.getDefaultTaxPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Tax percent must be from 0 to 100.", "/admin/settings");
        }
        if (form.getLowStockThreshold() < 0) {
            throw new BusinessException("Low stock threshold cannot be negative.", "/admin/settings");
        }
        if (form.getMaxUploadBytes() == null || form.getMaxUploadBytes() < 1 || form.getMaxUploadBytes() > 2097152) {
            throw new BusinessException("Upload size cannot exceed 2 MB.", "/admin/settings");
        }
        if (form.getSupportEmail() == null || !form.getSupportEmail().contains("@")) {
            throw new BusinessException("Enter a support email.", "/admin/settings");
        }
        if (!FieldRules.mobile(form.getSupportPhone())) {
            throw new BusinessException("Enter a 10-digit mobile number starting with 6, 7, 8, or 9.", "/admin/settings");
        }
        AppSetting setting = settings.findById(1L).orElseThrow();
        setting.setMallName(form.getMallName().trim());
        setting.setDefaultTaxPercent(Money.scale(form.getDefaultTaxPercent()));
        setting.setLowStockThreshold(form.getLowStockThreshold());
        setting.setMaxUploadBytes(form.getMaxUploadBytes());
        setting.setSupportEmail(form.getSupportEmail().trim().toLowerCase());
        setting.setSupportPhone(form.getSupportPhone().trim());
        setting.setUpdatedBy(guard.requireUser(adminId));
        settings.save(setting);
        audit.record(adminId, "SETTINGS_UPDATED", "app_settings", 1L, "Updated mall settings");
    }
}
