package com.shoppingmall.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.dto.RegistrationForm;
import com.shoppingmall.exception.DuplicateAccountException;
import com.shoppingmall.service.RegistrationService;
import com.shoppingmall.support.MallSpringTest;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RegistrationDuplicateTest extends MallSpringTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(5000);

    @Autowired
    private RegistrationService registration;

    @Test
    void duplicateEmailAndMobileAreRejected() {
        int n = SEQUENCE.incrementAndGet();
        RegistrationForm first = customer("user" + n + "@mall.test", mobile(n));
        registration.register(first);

        RegistrationForm sameEmail = customer(first.getEmail(), mobile(SEQUENCE.incrementAndGet()));
        DuplicateAccountException email = assertThrows(DuplicateAccountException.class, () -> registration.register(sameEmail));
        assertEquals("email", email.getField());
        assertEquals("An account with this email already exists.", email.getMessage());

        RegistrationForm sameMobile = customer("other" + n + "@mall.test", first.getMobile());
        DuplicateAccountException mobile = assertThrows(DuplicateAccountException.class, () -> registration.register(sameMobile));
        assertEquals("mobile", mobile.getField());
        assertEquals("An account with this mobile number already exists.", mobile.getMessage());
    }

    private static RegistrationForm customer(String email, String mobile) {
        RegistrationForm form = new RegistrationForm();
        form.setAccountType("CUSTOMER");
        form.setFullName("Riya Nair");
        form.setEmail(email);
        form.setMobile(mobile);
        form.setPassword("Testpass1");
        form.setConfirmPassword("Testpass1");
        form.setOpeningTime(LocalTime.of(10, 0));
        form.setClosingTime(LocalTime.of(21, 0));
        return form;
    }

    private static String mobile(int n) {
        return String.format("8%09d", n);
    }
}
