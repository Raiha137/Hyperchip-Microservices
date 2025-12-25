package com.hyperchip.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDtls entity
 * ----------------
 * Represents a user in the Hyperchip system.
 * Stores profile info, contact, verification, status flags, and addresses.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDtls {

    /**
     * Primary key for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to authentication service user id.
     * AuthService is the source of truth for login, password, and roles.
     */
    @Column(name = "auth_user_id", unique = true)
    private Long authUserId;

    // ---------------- Profile Information ----------------
    @Column(nullable = false)
    private String name;    // Full name of the user

    @Column(nullable = false)
    private String email;   // User email, required

    private String mobileNumber;   // Optional mobile number
    private String profileImage;   // URL/path to profile image

    // ---------------- Default Address Quick Fields (Optional) ----------------
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    // ---------------- Verification & Status Flags ----------------
    private Boolean emailVerified = false;  // true if email verified
    private Boolean phoneVerified = false;  // true if phone verified

    /**
     * Soft delete flag.
     * Marks user as deleted without removing the row physically.
     */
    private Boolean deleted = false;

    /**
     * Block flag.
     * Indicates if the user is blocked in the authentication service.
     */
    private Boolean blocked = false;

    // ---------------- Timestamps ----------------
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // creation timestamp

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(); // last updated timestamp

    /**
     * Updates the updatedAt timestamp automatically before any update.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------------- Relations ----------------
    /**
     * One-to-many relationship with Address entity.
     * Cascade all operations and remove orphaned addresses.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.hyperchip.user.model.Address> addresses = new ArrayList<>();
}
