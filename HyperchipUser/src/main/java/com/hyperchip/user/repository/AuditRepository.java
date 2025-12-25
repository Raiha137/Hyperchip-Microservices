package com.hyperchip.user.repository;

import com.hyperchip.user.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {
}
