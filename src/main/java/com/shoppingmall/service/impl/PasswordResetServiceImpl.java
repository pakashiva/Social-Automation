package com.shoppingmall.service.impl;

import com.shoppingmall.entity.PasswordResetToken;
import com.shoppingmall.validation.FieldRules;
import com.shoppingmall.entity.User;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.PasswordResetTokenRepository;
import com.shoppingmall.repository.UserRepository;
import com.shoppingmall.service.EmailNotificationPort;
import com.shoppingmall.service.PasswordResetService;
import com.shoppingmall.validation.PasswordRules;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final EmailNotificationPort email;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetServiceImpl(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            EmailNotificationPort email,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.email = email;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void issue(String emailAddress) {
        if (emailAddress == null) {
            return;
        }
        User user = users.findByEmail(emailAddress.trim().toLowerCase()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (PasswordResetToken existing : tokens.findUnused(user.getId())) {
            existing.setUsedAt(now);
        }
        byte[] rawBytes = new byte[32];
        random.nextBytes(rawBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(now.plusMinutes(30));
        tokens.save(token);
        email.sendPasswordReset(user.getId(), raw);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean peek(String token) {
        return valid(token) != null;
    }

    @Override
    @Transactional
    public void reset(String token, String password, String confirmPassword) {
        PasswordResetToken row = valid(token);
        if (row == null) {
            throw new BusinessException("This reset link is not valid.", "/reset-password");
        }
        if (!FieldRules.password(password) || password == null || !password.equals(confirmPassword)) {
            throw new BusinessException("Use 8 to 72 characters with at least one letter and one digit.", "/reset-password");
        }
        User user = row.getUser();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        row.setUsedAt(LocalDateTime.now(clock));
    }

    private PasswordResetToken valid(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        PasswordResetToken row = tokens.findByTokenHash(sha256(token)).orElse(null);
        if (row == null || row.getUsedAt() != null) {
            return null;
        }
        if (!row.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            return null;
        }
        if (row.getUser().getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        return row;
    }

    static String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
