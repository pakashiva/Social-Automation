package com.shoppingmall.security;

import com.shoppingmall.entity.User;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class MallAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String timingHash;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final MallUserDetailsService userDetailsService;

    public MallAuthenticationProvider(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            String timingHash,
            Clock clock,
            TransactionTemplate transactions,
            MallUserDetailsService userDetailsService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.timingHash = timingHash;
        this.clock = clock;
        this.transactions = transactions;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName() == null ? "" : authentication.getName().trim().toLowerCase();
        String raw = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(raw, timingHash);
            throw new BadCredentialsException("Invalid email or password.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            passwordEncoder.matches(raw, user.getPasswordHash());
            throw new LockedException("This account is locked. Try again in a few minutes.");
        }
        boolean matches = passwordEncoder.matches(raw, user.getPasswordHash());
        if (!matches) {
            if (user.getStatus() == UserStatus.ACTIVE) {
                transactions.executeWithoutResult(status -> recordFailure(user.getId(), now));
            }
            throw new BadCredentialsException("Invalid email or password.");
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new DisabledException("Your shop-owner account is waiting for admin approval.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException("This account is inactive. Contact the mall admin.");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new DisabledException("This registration was rejected.");
        }
        transactions.executeWithoutResult(status -> recordSuccess(user.getId(), now));
        MallUserDetails details = (MallUserDetails) userDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private void recordFailure(Long userId, LocalDateTime now) {
        User current = users.findById(userId).orElseThrow();
        int next = current.getFailedLoginCount() + 1;
        if (next >= 5) {
            current.setFailedLoginCount(0);
            current.setLockedUntil(now.plusMinutes(15));
        } else {
            current.setFailedLoginCount(next);
        }
        users.save(current);
    }

    private void recordSuccess(Long userId, LocalDateTime now) {
        User current = users.findById(userId).orElseThrow();
        current.setFailedLoginCount(0);
        current.setLockedUntil(null);
        current.setLastLoginAt(now);
        users.save(current);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
