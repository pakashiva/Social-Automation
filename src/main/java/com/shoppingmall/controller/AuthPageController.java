package com.shoppingmall.controller;

import com.shoppingmall.security.LoginFailureHandler;
import com.shoppingmall.security.LoginSuccessHandler;
import com.shoppingmall.security.MallUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String login(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof MallUserDetails details) {
            return "redirect:" + LoginSuccessHandler.home(details.getRole());
        }
        Object error = session.getAttribute(LoginFailureHandler.LOGIN_ERROR);
        if (error != null) {
            model.addAttribute("loginError", error);
            session.removeAttribute(LoginFailureHandler.LOGIN_ERROR);
        }
        return "public/login";
    }
}
