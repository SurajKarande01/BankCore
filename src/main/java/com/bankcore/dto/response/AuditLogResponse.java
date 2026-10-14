package com.bankcore.dto.response;

import com.bankcore.entity.AuditLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private String triggeredBy;
    private String details;
    private String status;
    private String ipAddress;
    private LocalDateTime timestamp;

    public static AuditLogResponse fromEntity(AuditLog audit) {
        return AuditLogResponse.builder()
                .id(audit.getId())
                .entityName(audit.getEntityName())
                .entityId(audit.getEntityId())
                .action(audit.getAction())
                .triggeredBy(audit.getTriggeredBy())
                .details(audit.getDetails())
                .status(audit.getStatus())
                .ipAddress(audit.getIpAddress())
                .timestamp(audit.getCreatedAt())
                .build();
    }
}
