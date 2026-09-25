package com.smartdesk.service;

import com.smartdesk.entity.AuditLog;
import com.smartdesk.entity.User;
import com.smartdesk.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(User actor, String action, String entityType, String entityId, String oldData, String newData, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog(actor, action, entityType, entityId, oldData, newData, ipAddress, userAgent);
            auditLogRepository.save(auditLog);
            log.info("AUDIT: action='{}' entityType='{}' entityId='{}' actor='{}'",
                    action, entityType, entityId, actor != null ? actor.getEmail() : "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage());
        }
    }
}
