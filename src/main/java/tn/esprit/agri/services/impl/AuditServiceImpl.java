package tn.esprit.agri.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.AuditLog;
import tn.esprit.agri.repositories.AuditLogRepository;
import tn.esprit.agri.services.AuditService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private static final int MAX_AUDIT_COLUMN_LENGTH = 255;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void log(String module, String action, Long entityId, Object beforeState, Object afterState, Long actorId) {
        AuditLog log = AuditLog.builder()
                .module(module)
                .action(action)
                .entityId(entityId)
                .beforeState(asJson(beforeState))
                .afterState(asJson(afterState))
                .actorId(actorId)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    private String asJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            return truncate(String.valueOf(value));
        }
    }

    private String truncate(String input) {
        if (input == null || input.length() <= MAX_AUDIT_COLUMN_LENGTH) {
            return input;
        }
        return input.substring(0, MAX_AUDIT_COLUMN_LENGTH);
    }
}
