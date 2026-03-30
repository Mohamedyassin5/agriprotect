package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.agri.entities.AuditLog;
import tn.esprit.agri.repositories.AuditLogRepository;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long entityId) {

        if (module != null && entityId != null) {
            return ResponseEntity.ok(auditLogRepository.findByModuleAndEntityIdOrderByCreatedAtDesc(module, entityId));
        }
        if (module != null) {
            return ResponseEntity.ok(auditLogRepository.findByModuleOrderByCreatedAtDesc(module));
        }
        if (entityId != null) {
            return ResponseEntity.ok(auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId));
        }
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc());
    }
}
