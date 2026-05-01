package tn.esprit.agri.controlleurs;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.Investigation;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.InvestigationType;
import tn.esprit.agri.services.InvestigationService;

import java.util.List;

@RestController
@RequestMapping("/api/investigations")
@RequiredArgsConstructor
public class InvestigationController {

    private final InvestigationService investigationService;

    // --- FARMER ENDPOINTS ---

    @PostMapping("/file")
    public ResponseEntity<?> fileInvestigation(@RequestBody FileInvestigationDTO dto, Authentication authentication) {
        try {
            User farmer = (User) authentication.getPrincipal();
            Investigation investigation = investigationService.fileInvestigation(
                    farmer.getId(),
                    dto.getRequestId(),
                    dto.getType(),
                    dto.getDescription()
            );
            return ResponseEntity.ok(investigation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- EXPERT ENDPOINTS ---

    @GetMapping("/expert/my-tasks")
    public ResponseEntity<?> getAssignedInvestigations(Authentication authentication) {
        try {
            User expert = (User) authentication.getPrincipal();
            List<Investigation> investigations = investigationService.getAssignedInvestigations(expert.getId());
            return ResponseEntity.ok(investigations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/expert/{investigationId}/decide")
    public ResponseEntity<?> decideOnInvestigation(
            @PathVariable String investigationId,
            @RequestBody DecideInvestigationDTO dto,
            Authentication authentication) {
        try {
            User expert = (User) authentication.getPrincipal();
            Investigation investigation = investigationService.decideOnInvestigation(
                    expert.getId(),
                    investigationId,
                    dto.isAccepted(),
                    dto.getDecisionReason()
            );
            return ResponseEntity.ok(investigation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- DTOs ---

    @Data
    public static class FileInvestigationDTO {
        private String requestId;
        private InvestigationType type; // INQUIRY or RECLAMATION
        private String description;
    }

    @Data
    public static class DecideInvestigationDTO {
        private boolean accepted;
        private String decisionReason;
    }
}
