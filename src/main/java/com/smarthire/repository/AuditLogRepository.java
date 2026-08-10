package com.smarthire.repository;

import com.smarthire.entity.AuditLog;
import com.smarthire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActor(User actor);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByActionContaining(String action);
}