package com.hyperchip.user.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * AuditLog entity
 * ----------------
 * Records changes/actions performed on entities (like User, Address, etc.)
 * by admins or users for auditing purposes.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * Primary key for audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the entity affected, e.g., "User", "Address".
     */
    private String entityName;

    /**
     * ID of the affected entity.
     */
    private Long entityId;

    /**
     * Action performed on the entity.
     * Examples: "BLOCK", "UNBLOCK", "SOFT_DELETE", "UPDATE", etc.
     */
    private String action;

    /**
     * Identifier of the person performing the action.
     * Can be an admin username or user email.
     */
    private String performedBy;

    /**
     * Timestamp of when the action occurred.
     * Defaults to current time at entity creation.
     */
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Optional detailed description or additional information
     * about the action performed.
     */
    private String detail;
}
