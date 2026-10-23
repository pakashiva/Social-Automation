package com.shoppingmall.service.impl;

import com.shoppingmall.dto.PasswordChangeForm;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.dto.ProfileForm;
import com.shoppingmall.dto.Rows;
import com.shoppingmall.entity.Customer;
import com.shoppingmall.entity.User;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.CustomerRepository;
import com.shoppingmall.security.OwnershipGuard;
import com.shoppingmall.service.ProfileService;
import com.shoppingmall.util.Pages;
import com.shoppingmall.validation.PasswordRules;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final OwnershipGuard guard;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(OwnershipGuard guard, CustomerRepository customers, PasswordEncoder passwordEncoder) {
        this.guard = guard;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileForm get(long userId) {
        Customer customer = guard.requireCustomer(userId);
        User user = customer.getUser();
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setMobile(user.getMobile());
        form.setGender(customer.getGender());
        form.setDateOfBirth(customer.getDateOfBirth());
        form.setAddressLine1(customer.getAddressLine1());
        form.setAddressLine2(customer.getAddressLine2());
        form.setCity(customer.getCity());
        form.setState(customer.getState());
        form.setPostalCode(customer.getPostalCode());
        form.setCountry(customer.getCountry());
        return form;
    }

    @Override
    @Transactional
    public void update(long userId, ProfileForm form) {
        Customer customer = guard.requireCustomer(userId);
        User user = customer.getUser();
        user.setFullName(form.getFullName().trim());
        customer.setGender(form.getGender());
        customer.setDateOfBirth(form.getDateOfBirth());
        customer.setAddressLine1(trim(form.getAddressLine1()));
        customer.setAddressLine2(trim(form.getAddressLine2()));
        customer.setCity(trim(form.getCity()));
        customer.setState(trim(form.getState()));
        customer.setPostalCode(trim(form.getPostalCode()));
        customer.setCountry(form.getCountry() == null || form.getCountry().isBlank() ? "India" : form.getCountry().trim());
    }

    @Override
    @Transactional
    public void changePassword(long userId, PasswordChangeForm form) {
        User user = guard.requireCustomer(userId).getUser();
        if (form.getCurrentPassword() == null || !passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("The current password is wrong.", "/customer/profile");
        }
        if (!FieldRules.password(form.getNewPassword())) {
            throw new BusinessException("Use 8 to 72 characters with at least one letter and one digit.", "/customer/profile");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Confirm the new password.", "/customer/profile");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rows.UserRow> searchCustomers(String query, int page) {
        String q = query == null || query.isBlank() ? "" : "%" + query.trim().toLowerCase() + "%";
        return customers.search(q, Pages.of(page)).map(customer -> {
            Rows.UserRow row = new Rows.UserRow();
            User user = customer.getUser();
            row.setId(user.getId());
            row.setEmail(user.getEmail());
            row.setMobile(user.getMobile());
            row.setFullName(user.getFullName());
            row.setStatus(user.getStatus().name());
            row.setRole("CUSTOMER");
            row.setAddress(address(customer));
            return row;
        });
    }

    private static String address(Customer customer) {
        StringBuilder builder = new StringBuilder();
        append(builder, customer.getAddressLine1());
        append(builder, customer.getCity());
        append(builder, customer.getPostalCode());
        return builder.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
