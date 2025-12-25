package com.hyperchip.admin.dto;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User Entity
 *
 * Represents a user in the system.
 * Includes personal info, authentication credentials, role, and status flags.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"), // email must be unique
                @UniqueConstraint(columnNames = "phone") // optional: phone must be unique
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // ----------------------------
    // PRIMARY KEY
    // ----------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // unique identifier for the user

    // ----------------------------
    // PERSONAL INFO
    // ----------------------------
    @Column(nullable = false, length = 100)
    private String name;  // full name of the user

    @Column(nullable = false, unique = true, length = 150)
    private String email; // unique email for login

    @Column(length = 20)
    private String phone; // optional unique phone number

    @Column(length = 500)
    private String profileImage; // URL or path to user's profile picture

    // ----------------------------
    // AUTHENTICATION
    // ----------------------------
    @Column(nullable = false, length = 255)
    private String password; // hashed password

    // ----------------------------
    // ACCOUNT STATUS FLAGS
    // ----------------------------
    @Builder.Default
    private boolean blocked = false; // admin can block the user

    @Builder.Default
    private boolean deleted = false; // soft delete, true if user is deleted

    // ----------------------------
    // ROLE
    // ----------------------------
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String role = "USER"; // e.g., USER or ADMIN

    // ----------------------------
    // TIMESTAMPS
    // ----------------------------
    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // auto-set when inserted

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt; // auto-set on update
}
