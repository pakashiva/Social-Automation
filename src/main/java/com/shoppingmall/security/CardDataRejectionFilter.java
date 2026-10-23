package com.shoppingmall.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CardDataRejectionFilter extends OncePerRequestFilter {

    private static final Set<String> REJECTED = Set.of("cardnumber", "card_number", "pan", "cvv", "cvc", "expiry");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        for (String name : request.getParameterMap().keySet()) {
            if (REJECTED.contains(name.toLowerCase(Locale.ROOT))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Card data is not accepted.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
