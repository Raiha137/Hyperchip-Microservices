package com.hyperchip.auth.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * User entity
 * - Represents a user account in the system.
 * - Stores credentials, profile info, and account status.
 * - Handles regular login, signup, and OAuth2 login.
 * - Includes flags like enabled, active, blocked, accountNonLocked for security.
 * - Uses explicit boolean getters to avoid Lombok boolean naming confusion.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key, auto-generated

    @Column(name = "full_name", nullable = false)
    private String fullName; // User-friendly name, required

    @Column(nullable = false, unique = true)
    private String email; // Unique email, used as username

    @Column(nullable = false)
    private String password; // Encrypted password, required

    @Column
    private String phone; // Optional phone number

    @Column(nullable = false)
    private Boolean enabled = true; // True if account is enabled

    @Column
    private Boolean blocked = false; // True if account is blocked

    @Column
    private String blockReason; // Optional reason for blocking

    @Column(nullable = false)
    private Boolean active = true; // True if account is active

    @Column(nullable = false)
    private Boolean accountNonLocked = true; // True if account is not locked

    @Column(nullable = false)
    private String role = "ROLE_USER"; // Role of user (ROLE_USER / ROLE_ADMIN)

    @Column
    private String provider; // OAuth2 provider name (google, github, etc.)

    // Explicit getters/setters for booleans to avoid Lombok naming ambiguity
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getAccountNonLocked() { return accountNonLocked; }
    public void setAccountNonLocked(Boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }

    // Convenience boolean methods for Spring Security or legacy code
    public boolean isBlocked() { return Boolean.TRUE.equals(this.blocked); }
    public boolean isActive() { return Boolean.TRUE.equals(this.active); }
    public boolean isEnabled() { return Boolean.TRUE.equals(this.enabled); }
    public boolean isAccountNonLocked() { return Boolean.TRUE.equals(this.accountNonLocked); }
}
