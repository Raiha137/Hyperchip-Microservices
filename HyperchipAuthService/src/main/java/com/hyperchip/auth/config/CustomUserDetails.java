package com.hyperchip.auth.config;

import com.hyperchip.auth.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * CustomUserDetails
 *
 * Simple adapter that wraps our User entity and exposes it to Spring Security.
 * - Provides authorities from user role
 * - Controls account/credential/lock/enabled checks based on user flags
 *
 * Developer notes (simple):
 * - getUsername() returns user's email
 * - getAuthorities() returns a single authority built from user.getRole()
 * - isAccountNonLocked() treats 'blocked' as locked and respects accountNonLocked flag
 * - isEnabled() requires active + enabled and not blocked
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Return granted authorities for this user.
     * Uses user.getRole() as a single authority.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole()));
    }

    /**
     * Return the user's password (hashed).
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Return the username used for authentication.
     * We use email as username.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Not using account expiration logic, always return true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account is considered non-locked only when:
     * - accountNonLocked is true (or non-null true), AND
     * - blocked flag is NOT true
     *
     * This treats a blocked user as locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getAccountNonLocked()) && !Boolean.TRUE.equals(user.getBlocked());
    }

    /**
     * Not using credential-expiration logic, always return true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * User is enabled only when:
     * - active flag is true
     * - enabled flag is true
     * - and user is NOT blocked
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActive())
                && Boolean.TRUE.equals(user.getEnabled())
                && !Boolean.TRUE.equals(user.getBlocked());
    }
}
