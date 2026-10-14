package com.bankcore.service;

import com.bankcore.dto.response.AuditLogResponse;

import java.util.List;

public interface AuditService {
    void log(String entityName, Long entityId, String action, String triggeredBy, String details, String status, String ipAddress);

    List<AuditLogResponse> getLogsForEntity(String entityName, Long entityId);

    List<AuditLogResponse> getLogsByActor(String actor);

    List<AuditLogResponse> getAllLogs();
}
