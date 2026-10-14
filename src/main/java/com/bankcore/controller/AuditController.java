package com.bankcore.controller;

import com.bankcore.dto.response.ApiResponse;
import com.bankcore.dto.response.AuditLogResponse;
import com.bankcore.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query immutable audit logs tracking operations, actors, and state transitions")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get all audit logs ordered newest to oldest")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAllAuditLogs() {
        List<AuditLogResponse> logs = auditService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved", logs));
    }

    @GetMapping("/actor/{actor}")
    @Operation(summary = "Get audit logs filtered by who triggered them")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByActor(@PathVariable String actor) {
        List<AuditLogResponse> logs = auditService.getLogsByActor(actor);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs for actor retrieved", logs));
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsForEntity(
            @PathVariable String entityName,
            @PathVariable Long entityId
    ) {
        List<AuditLogResponse> logs = auditService.getLogsForEntity(entityName, entityId);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs for entity retrieved", logs));
    }
}
