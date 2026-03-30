package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);
    List<AuditLog> findByModuleAndEntityIdOrderByCreatedAtDesc(String module, Long entityId);
    List<AuditLog> findByEntityIdOrderByCreatedAtDesc(Long entityId);
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
