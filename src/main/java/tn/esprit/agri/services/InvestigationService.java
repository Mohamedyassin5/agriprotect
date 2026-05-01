package tn.esprit.agri.services;

import tn.esprit.agri.entities.Investigation;
import tn.esprit.agri.entities.enums.InvestigationType;

import java.util.List;

public interface InvestigationService {
    Investigation fileInvestigation(String farmerId, String requestId, InvestigationType type, String description);
    List<Investigation> getAssignedInvestigations(String expertId);
    Investigation decideOnInvestigation(String expertId, String investigationId, boolean accepted, String decisionReason);
}
