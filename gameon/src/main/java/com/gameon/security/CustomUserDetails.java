package com.gameon.security;

import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation wrapping the GameOn User entity.
 * Provides Spring Security integration with the application's user model.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map UserRole to Spring Security role format (ROLE_ prefix)
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getIsActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }

    // ===== Custom accessors =====

    public Long getUserId() {
        return user.getUserId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public UserRole getUserRole() {
        return user.getUserRole();
    }

    public User getUser() {
        return user;
    }

    public boolean isModerator() {
        return user.getUserRole() == UserRole.MODERATOR;
    }

    public boolean isAdmin() {
        return user.getUserRole() == UserRole.ADMIN;
    }
}
