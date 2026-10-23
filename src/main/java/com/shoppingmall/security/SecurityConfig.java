package com.shoppingmall.security;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public String timingHash(PasswordEncoder passwordEncoder) {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }

    @Bean
    public AuthenticationManager authenticationManager(MallAuthenticationProvider provider) {
        return new ProviderManager(List.of(provider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            LoginSuccessHandler successHandler,
            LoginFailureHandler failureHandler,
            CardDataRejectionFilter cardDataRejectionFilter)
            throws Exception {
        http.authenticationManager(authenticationManager);
        http.csrf(csrf -> {});
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionFixation().changeSessionId());
        http.addFilterBefore(cardDataRejectionFilter, CsrfFilter.class);
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                if ("GET".equalsIgnoreCase(request.getMethod())
                        && request.getRequestURI() != null
                        && request.getRequestURI().endsWith("/logout")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                chain.doFilter(request, response);
            }
        }, LogoutFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/css/**", "/js/**", "/vendor/**", "/images/**", "/error", "/media/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/shops", "/shops/**", "/products", "/products/**",
                        "/categories", "/categories/**", "/offers", "/contact", "/login", "/register",
                        "/forgot-password", "/reset-password")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/contact", "/register", "/forgot-password", "/reset-password")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/logout").authenticated()
                .requestMatchers(HttpMethod.GET, "/logout").denyAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/owner/**").hasRole("SHOP_OWNER")
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .anyRequest().authenticated());
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler));
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"));
        http.exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> response.sendError(403)));
        return http.build();
    }
}
