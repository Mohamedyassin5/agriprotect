package tn.esprit.agri.controlleurs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.RemboursementDTO;
import tn.esprit.agri.entities.Remboursement;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.StatutRemboursement;
import tn.esprit.agri.services.IRemboursementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agri/remboursements")
@RequiredArgsConstructor
@Tag(name = "Remboursements", description = "Gestion des demandes de remboursement agricole")
public class RemboursementController {

    private final IRemboursementService remboursementService;

    // =========================================================================
    // FARMER — Simulation (dry-run, aucune persistance)
    // =========================================================================

    /**
     * Permet au farmer de voir combien il recevrait AVANT de soumettre une vraie demande.
     * Aucune donnée n'est enregistrée, aucun versement n'est effectué.
     */
    @GetMapping("/simulate/{sinistreId}")
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Simuler un remboursement sans le soumettre")
    public ResponseEntity<?> simulerRemboursement(
            @PathVariable String sinistreId,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        try {
            RemboursementDTO dto = remboursementService.simulerRemboursement(sinistreId, user.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.warn("Simulation impossible — sinistre {} : {}", sinistreId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // FARMER — Soumettre une demande
    // =========================================================================

    /**
     * Flux professionnel :
     *  • Montant ≤ 200 TND  → versement automatique immédiat (micro-remboursement)
     *  • Montant >  200 TND → statut EN_ATTENTE, validation admin obligatoire
     */
    @PostMapping("/sinistre/{sinistreId}")
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Soumettre une demande de remboursement pour un sinistre")
    public ResponseEntity<?> soumettreRemboursement(
            @PathVariable String sinistreId,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        try {
            RemboursementDTO dto = remboursementService.soumettreDemandeRemboursement(sinistreId, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            log.error("Erreur soumission sinistre {} par user {}: {}", sinistreId, user.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // FARMER — Annuler une demande EN_ATTENTE
    // =========================================================================

    @DeleteMapping("/{remboursementId}/cancel")
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Annuler une demande de remboursement en attente")
    public ResponseEntity<?> annulerRemboursement(
            @PathVariable String remboursementId,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        try {
            remboursementService.annulerDemande(remboursementId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Demande annulée avec succès."));
        } catch (Exception e) {
            log.warn("Annulation refusée pour {} : {}", remboursementId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // FARMER — Mes remboursements
    // =========================================================================

    @GetMapping("/my")
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Récupérer tous mes remboursements")
    public ResponseEntity<List<Remboursement>> getMyRemboursements(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(remboursementService.getRemboursementsByUser(user.getId()));
    }

    @GetMapping("/{remboursementId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(summary = "Voir le détail d'un remboursement")
    public ResponseEntity<?> getRemboursementById(
            @PathVariable String remboursementId,
            Authentication auth) {

        User user = (User) auth.getPrincipal();
        try {
            Remboursement remboursement = remboursementService.getRemboursementById(remboursementId, user);
            return ResponseEntity.ok(remboursement);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // ADMIN — Liste paginée avec filtre statut
    // =========================================================================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste paginée de tous les remboursements (Admin)")
    public ResponseEntity<Page<Remboursement>> getAllRemboursements(
            @RequestParam(required = false) StatutRemboursement statut,
            @PageableDefault(size = 20, sort = "dateRemboursement") Pageable pageable) {

        return ResponseEntity.ok(remboursementService.getAllRemboursements(statut, pageable));
    }

    // =========================================================================
    // ADMIN — Remboursements d'un farmer spécifique
    // =========================================================================

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Voir tous les remboursements d'un agriculteur (Admin)")
    public ResponseEntity<List<Remboursement>> getRemboursementsByUser(
            @PathVariable String userId) {

        return ResponseEntity.ok(remboursementService.getRemboursementsByUserId(userId));
    }

    // =========================================================================
    // ADMIN — Approuver + verser le montant
    // =========================================================================

    /**
     * L'admin peut ajuster le montant final avant de valider (ex : après expertise terrain).
     * Si montantAjuste est absent, le montant calculé automatiquement est utilisé.
     */
    @PostMapping("/{remboursementId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approuver et verser un remboursement (Admin)")
    public ResponseEntity<?> approveRemboursement(
            @PathVariable String remboursementId,
            @RequestParam(required = false) BigDecimal montantAjuste,
            Authentication auth) {

        User admin = (User) auth.getPrincipal();
        try {
            Remboursement remboursement = remboursementService.approuverRemboursement(
                    remboursementId, admin.getId(), montantAjuste);
            return ResponseEntity.ok(remboursement);
        } catch (Exception e) {
            log.error("Erreur approbation {} : {}", remboursementId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // ADMIN — Refuser un remboursement
    // =========================================================================

    @PostMapping("/{remboursementId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refuser un remboursement avec motif obligatoire (Admin)")
    public ResponseEntity<?> rejectRemboursement(
            @PathVariable String remboursementId,
            @RequestParam String motif,
            Authentication auth) {

        try {
            Remboursement remboursement = remboursementService.refuserRemboursement(remboursementId, motif);
            return ResponseEntity.ok(remboursement);
        } catch (Exception e) {
            log.error("Erreur refus {} : {}", remboursementId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // ADMIN — Remboursements suspects (anti-fraude)
    // =========================================================================

    @GetMapping("/admin/suspicious")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste des remboursements suspects (Anti-fraude)")
    public ResponseEntity<List<Remboursement>> getSuspiciousRemboursements() {
        return ResponseEntity.ok(remboursementService.getRemboursementsSuspects());
    }

    // =========================================================================
    // ADMIN — Statistiques
    // =========================================================================

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques globales des remboursements")
    public ResponseEntity<Map<String, Object>> getRemboursementStats() {
        return ResponseEntity.ok(remboursementService.getRemboursementStats());
    }
}
