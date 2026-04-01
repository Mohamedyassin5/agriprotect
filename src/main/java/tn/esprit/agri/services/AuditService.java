package tn.esprit.agri.services;

public interface AuditService {
    void log(String module, String action, Long entityId, Object beforeState, Object afterState, Long actorId);
}
