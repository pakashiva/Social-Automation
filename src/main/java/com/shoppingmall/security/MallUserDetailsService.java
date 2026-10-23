package com.shoppingmall.security;

import com.shoppingmall.entity.User;
import com.shoppingmall.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MallUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final Clock clock;

    public MallUserDetailsService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email = username == null ? "" : username.trim().toLowerCase();
        User user = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now(clock));
        var role = user.getUserRole().getRole().getName();
        return new MallUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getStatus(),
                role,
                locked);
    }
}
