package com.shoppingmall.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoppingmall.support.MallSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class SecurityUrlTest extends MallSpringTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousHomeIsPublicAndAdminRequiresLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Home")));
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void customerCannotOpenAdminAndGetLogoutIsDenied() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("shopper@mall.test").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/logout")).andExpect(status().isForbidden());
    }

    @Test
    void cardParameterIsRejected() throws Exception {
        mockMvc.perform(post("/login").with(csrf()).param("cardNumber", "4111111111111111")
                        .param("username", "a@mall.test").param("password", "Testpass1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Card data is not accepted."));
    }
}
