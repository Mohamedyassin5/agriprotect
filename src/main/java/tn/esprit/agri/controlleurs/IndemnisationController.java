package tn.esprit.agri.controlleurs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Data; // Added
import lombok.Builder; // Added
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.IndemnisationRequest;
import tn.esprit.agri.services.IndemnisationService;

@RestController
@RequestMapping("/api/indemnisation")
@RequiredArgsConstructor
public class IndemnisationController {

    private final IndemnisationService indemnisationService;

    /**
     * Endpoint pour créer une demande d'indemnisation
     * Exemple POST JSON :
     * {
     * "fundId": "uuid_fund",
     * "amount": 500.0,
     * "reason": "Perte de récolte"
     * }
     */
    @PostMapping(value = "/request", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> requestIndemnisation(
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            @RequestParam("fundId") String fundId,
            @RequestParam("amount") Double amount,
            @RequestParam("reason") String reason,
            org.springframework.security.core.Authentication authentication) {
        try {
            tn.esprit.agri.entities.User user = (tn.esprit.agri.entities.User) authentication.getPrincipal();
            IndemnisationRequest request = indemnisationService.requestIndemnisation(
                    user.getId(),
                    fundId,
                    amount,
                    reason,
                    image);
            return ResponseEntity.ok(request);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // DTO pour recevoir les données du POST
    @Getter
    @Setter
    public static class RequestDTO {
        private String fundId;
        private Double amount;
        private String reason;
    }

    @GetMapping("/pending")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment if security is enabled for
    // Admin only
    public ResponseEntity<java.util.List<PendingRequestDTO>> getAllPendingRequests() {
        java.util.List<IndemnisationRequest> requests = indemnisationService.getAllPendingRequests();
        java.util.List<PendingRequestDTO> dtos = requests.stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/my-history")
    // @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<java.util.List<IndemnisationRequest>> getMyHistory(
            org.springframework.security.core.Authentication authentication) {
        tn.esprit.agri.entities.User farmer = (tn.esprit.agri.entities.User) authentication.getPrincipal();
        return ResponseEntity.ok(indemnisationService.getFarmerRequestHistory(farmer.getId()));
    }

    @GetMapping("/memberships")
    public ResponseEntity<java.util.List<tn.esprit.agri.entities.FarmerSolidarityFund>> getMyMemberships(
            org.springframework.security.core.Authentication authentication) {
        tn.esprit.agri.entities.User farmer = (tn.esprit.agri.entities.User) authentication.getPrincipal();
        return ResponseEntity.ok(indemnisationService.getMemberships(farmer.getId()));
    }

    private PendingRequestDTO mapToDTO(IndemnisationRequest request) {
        return PendingRequestDTO.builder()
                .id(request.getId())
                .farmerName(request.getFarmer().getFirstName() + " " + request.getFarmer().getLastName())
                .fundName(request.getFund().getName())
                .amount(request.getRequestedAmount())
                .reason(request.getRequestReason())
                .date(request.getRequestDate())
                .aiScore(request.getAiScore())
                .aiAnalysis(request.getAiAnalysis())
                .build();
    }

    @Data
    @Builder
    public static class PendingRequestDTO {
        private String id;
        private String farmerName;
        private String fundName;
        private Double amount;
        private String reason;
        private java.time.LocalDateTime date;
        private Double aiScore;
        private String aiAnalysis;
    }

    @PutMapping("/{requestId}/process")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processRequest(
            @PathVariable String requestId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String refusalReason) {
        try {
            indemnisationService.processIndemnisation(requestId, approved, refusalReason);
            return ResponseEntity.ok("Request processed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
