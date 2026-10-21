package com.bankcore.service.impl;

import com.bankcore.dto.response.AuditLogResponse;
import com.bankcore.entity.AuditLog;
import com.bankcore.repository.AuditLogRepository;
import com.bankcore.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String entityName, Long entityId, String action, String triggeredBy, String details, String status, String ipAddress) {
        String safeEntity = entityName != null ? entityName : "UNKNOWN";
        String safeAction = action != null ? action : "UNKNOWN";
        String actor = (triggeredBy != null && !triggeredBy.trim().isEmpty()) ? triggeredBy.trim() : "SYSTEM";
        String logStatus = status != null ? status : "SUCCESS";
        String ip = (ipAddress != null && !ipAddress.trim().isEmpty()) ? ipAddress.trim() : "127.0.0.1";

        AuditLog auditLog = AuditLog.builder()
                .entityName(safeEntity)
                .entityId(entityId)
                .action(safeAction)
                .triggeredBy(actor)
                .details(details)
                .status(logStatus)
                .ipAddress(ip)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("[AUDIT] Action: {} on Entity: {} (ID: {}) by Actor: '{}' - Status: {}",
                safeAction, safeEntity, entityId, actor, logStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsForEntity(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByActor(String actor) {
        String cleanActor = actor != null ? actor.trim() : "";
        return auditLogRepository.findByTriggeredByOrderByCreatedAtDesc(cleanActor)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }
}

