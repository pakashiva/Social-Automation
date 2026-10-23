package com.shoppingmall.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.support.MallSpringTest;
import com.shoppingmall.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class LoginStatusTest extends MallSpringTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownEmailAndWrongPasswordShareTheSameMessage() throws Exception {
        assertLoginMessage("missing@mall.test", TestData.PASSWORD, "Invalid email or password.");
        TestData.Person customer = data.customer();
        assertLoginMessage(customer.email(), "wrong-pass", "Invalid email or password.");
    }

    @Test
    void statusMessagesAreShownOnlyAfterThePasswordMatches() throws Exception {
        TestData.Person pending = data.person(RoleName.SHOP_OWNER, UserStatus.PENDING);
        TestData.Person inactive = data.person(RoleName.CUSTOMER, UserStatus.INACTIVE);
        TestData.Person rejected = data.person(RoleName.SHOP_OWNER, UserStatus.REJECTED);
        assertLoginMessage(pending.email(), TestData.PASSWORD, "Your shop-owner account is waiting for admin approval.");
        assertLoginMessage(inactive.email(), TestData.PASSWORD, "This account is inactive. Contact the mall admin.");
        assertLoginMessage(rejected.email(), TestData.PASSWORD, "This registration was rejected.");
    }

    @Test
    void activeCustomerSignsInAndTheFifthFailureLocksTheAccount() throws Exception {
        TestData.Person customer = data.customer();
        mockMvc.perform(post("/login").with(csrf()).param("username", customer.email()).param("password", TestData.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));

        TestData.Person locked = data.customer();
        for (int attempt = 0; attempt < 5; attempt++) {
            assertLoginMessage(locked.email(), "wrong-pass", "Invalid email or password.");
        }
        assertLoginMessage(locked.email(), TestData.PASSWORD, "This account is locked. Try again in a few minutes.");
    }

    private void assertLoginMessage(String email, String password, String message) throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(message)));
    }
}
