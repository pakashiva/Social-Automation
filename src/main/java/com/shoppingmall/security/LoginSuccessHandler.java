package com.shoppingmall.security;

import com.shoppingmall.enums.RoleName;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        MallUserDetails details = (MallUserDetails) authentication.getPrincipal();
        response.sendRedirect(request.getContextPath() + home(details.getRole()));
    }

    public static String home(RoleName role) {
        if (role == RoleName.ADMIN) {
            return "/admin/dashboard";
        }
        if (role == RoleName.SHOP_OWNER) {
            return "/owner/dashboard";
        }
        if (role == RoleName.CUSTOMER) {
            return "/customer/dashboard";
        }
        return "/";
    }
}
