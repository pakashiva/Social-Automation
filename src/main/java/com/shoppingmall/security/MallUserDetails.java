package com.shoppingmall.security;

import com.shoppingmall.enums.RoleName;
import com.shoppingmall.enums.UserStatus;
import com.shoppingmall.exception.ForbiddenException;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MallUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserStatus status;
    private final RoleName role;
    private final boolean locked;

    public MallUserDetails(
            Long id,
            String email,
            String passwordHash,
            String fullName,
            UserStatus status,
            RoleName role,
            boolean locked) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.status = status;
        this.role = role;
        this.locked = locked;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public RoleName getRole() {
        return role;
    }

    public static long requireId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof MallUserDetails details) {
            return details.getId();
        }
        throw new ForbiddenException();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
