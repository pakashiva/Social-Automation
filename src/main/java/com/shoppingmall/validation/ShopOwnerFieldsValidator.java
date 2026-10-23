package com.shoppingmall.validation;

import com.shoppingmall.dto.RegistrationForm;
import com.shoppingmall.validation.FieldRules;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ShopOwnerFieldsValidator implements ConstraintValidator<ShopOwnerFields, RegistrationForm> {

    @Override
    public boolean isValid(RegistrationForm form, ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean ok = true;
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            violation(context, "confirmPassword", "Password confirmation does not match.");
            ok = false;
        }
        if (form.getAccountType() == null) {
            return ok;
        }
        boolean owner = "SHOP_OWNER".equals(form.getAccountType());
        boolean customer = "CUSTOMER".equals(form.getAccountType());
        if (!owner && !customer) {
            violation(context, "accountType", "Choose customer or shop owner.");
            return false;
        }
        boolean shopFieldsUsed = notBlank(form.getBusinessName())
                || notBlank(form.getGstNumber())
                || notBlank(form.getShopName())
                || notBlank(form.getFloorLabel())
                || notBlank(form.getUnitNumber())
                || notBlank(form.getShopPhone())
                || form.getOpeningTime() != null
                || form.getClosingTime() != null;
        if (customer && shopFieldsUsed) {
            violation(context, "accountType", "Shop fields are only for shop-owner registration.");
            return false;
        }
        if (!owner) {
            return ok;
        }
        ok &= required(context, form.getBusinessName(), "businessName", "Enter the business name.");
        ok &= required(context, form.getShopName(), "shopName", "Enter the shop name.");
        ok &= required(context, form.getFloorLabel(), "floorLabel", "Enter the floor.");
        ok &= required(context, form.getUnitNumber(), "unitNumber", "Enter the unit number.");
        if (!FieldRules.mobile(form.getShopPhone())) {
            violation(context, "shopPhone", "Enter a 10-digit mobile number starting with 6, 7, 8, or 9.");
            ok = false;
        }
        if (form.getOpeningTime() == null || form.getClosingTime() == null
                || !form.getOpeningTime().isBefore(form.getClosingTime())) {
            violation(context, "closingTime", "Closing time must be after opening time.");
            ok = false;
        }
        if (notBlank(form.getGstNumber()) && !FieldRules.gst(form.getGstNumber().trim().toUpperCase())) {
            violation(context, "gstNumber", "Enter a valid GST number or leave it blank.");
            ok = false;
        }
        return ok;
    }

    private static boolean required(ConstraintValidatorContext context, String value, String field, String message) {
        if (notBlank(value)) {
            return true;
        }
        violation(context, field, message);
        return false;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void violation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
    }
}
