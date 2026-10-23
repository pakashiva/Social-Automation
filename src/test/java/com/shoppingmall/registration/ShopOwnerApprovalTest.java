package com.shoppingmall.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shoppingmall.dto.RegistrationForm;
import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.service.RegistrationService;
import com.shoppingmall.service.ShopService;
import com.shoppingmall.service.UserAdminService;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ShopOwnerApprovalTest extends MallSpringTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(7000);

    @Autowired
    private RegistrationService registration;

    @Autowired
    private ShopService shops;

    @Autowired
    private UserAdminService users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void approveActivatesTheOwnerAndRejectLeavesTheShopDraft() {
        TestData.Person admin = data.person(RoleName.ADMIN, UserStatus.ACTIVE);
        long approvedUser = registerOwner();
        long shopId = data.shopIdForUser(approvedUser);
        shops.approve(admin.userId(), shopId);
        assertEquals("ACTIVE", data.userStatus(approvedUser));
        assertEquals("APPROVED", data.approvalStatus(approvedUser));
        assertEquals("ACTIVE", data.shopStatus(shopId));

        long rejectedUser = registerOwner();
        long rejectedShop = data.shopIdForUser(rejectedUser);
        shops.reject(admin.userId(), rejectedShop, "The documents do not match the unit.");
        assertEquals("REJECTED", data.userStatus(rejectedUser));
        assertEquals("REJECTED", data.approvalStatus(rejectedUser));
        assertEquals("DRAFT", data.shopStatus(rejectedShop));
    }

    @Test
    void statusChangeCannotApproveAndCannotRemoveTheLastAdmin() {
        TestData.Person admin = data.person(RoleName.ADMIN, UserStatus.ACTIVE);
        long pending = registerOwner();
        BusinessException approval = assertThrows(BusinessException.class,
                () -> users.changeStatus(admin.userId(), pending, UserStatus.ACTIVE));
        assertEquals("Approve the shop owner from the shop screen.", approval.getMessage());

        data.isolateAdmin(admin.userId(), () -> {
            BusinessException last = assertThrows(BusinessException.class,
                    () -> users.changeStatus(admin.userId(), admin.userId(), UserStatus.INACTIVE));
            assertEquals("The last active admin cannot be deactivated.", last.getMessage());
        });
    }

    private long registerOwner() {
        int n = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setAccountType("SHOP_OWNER");
        form.setFullName("Vikram Das");
        form.setEmail("owner" + n + "@mall.test");
        form.setMobile(String.format("7%09d", n));
        form.setPassword("Testpass1");
        form.setConfirmPassword("Testpass1");
        form.setBusinessName("Pending Tailors " + n);
        form.setShopName("Pending Tailors " + n);
        form.setFloorLabel("P" + n);
        form.setUnitNumber(String.valueOf(n));
        form.setShopPhone(String.format("6%09d", n));
        form.setOpeningTime(LocalTime.of(10, 0));
        form.setClosingTime(LocalTime.of(20, 0));
        registration.register(form);
        return jdbc.queryForObject("select id from users where email = ?", Long.class, form.getEmail());
    }
}
