package com.hyperchip.user.service;

import com.hyperchip.user.model.AuditLog;
import com.hyperchip.user.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for recording audit logs in the system.
 *
 * Purpose:
 * - Provides a centralized way to log actions performed on entities by users.
 * - Helps track changes, modifications, and important operations for security and debugging.
 */
@Service
@RequiredArgsConstructor // Generates constructor for final fields (auditRepository)
public class AuditService {

    /**
     * Repository for saving AuditLog entities to the database.
     */
    private final AuditRepository auditRepository;

    /**
     * Create and save a new audit log entry.
     *
     * Purpose:
     * - Logs a specific action performed on an entity (e.g., create, update, delete).
     * - Records which user performed the action and any additional details.
     * - Stores the timestamp of when the action occurred.
     *
     * Usage:
     * - Can be called from controllers or services whenever an important entity action occurs.
     * - Useful for auditing, debugging, and security tracking.
     *
     * @param entityName  Name of the entity (e.g., "User", "Product")
     * @param entityId    ID of the entity being affected
     * @param action      Action performed (e.g., "CREATE", "UPDATE", "DELETE")
     * @param performedBy User or system performing the action
     * @param detail      Additional details about the action
     * @return The saved AuditLog entity
     */
    public AuditLog log(String entityName,
                        Long entityId,
                        String action,
                        String performedBy,
                        String detail) {

        // Build a new AuditLog object using Lombok's builder
        AuditLog auditLog = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .detail(detail)
                .timestamp(LocalDateTime.now()) // Record the current timestamp
                .build();

        // Save the audit log entry to the database
        return auditRepository.save(auditLog);
    }
}
