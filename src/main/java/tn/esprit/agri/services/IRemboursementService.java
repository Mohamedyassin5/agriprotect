package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.DTO.RemboursementDTO;
import tn.esprit.agri.entities.Remboursement;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.StatutRemboursement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IRemboursementService {

    // ── FARMER ───────────────────────────────────────────────────────────────────

    /** Simulation dry-run : calcule le montant estimé sans rien persister. */
    RemboursementDTO simulerRemboursement(String sinistreId, String userId);

    /** Soumet une vraie demande : persiste EN_ATTENTE ou PAYE (micro-remboursement). */
    RemboursementDTO soumettreDemandeRemboursement(String sinistreId, String userId);

    /** Annule une demande EN_ATTENTE. */
    void annulerDemande(String remboursementId, String userId);

    /** Retourne tous les remboursements d'un farmer. */
    List<Remboursement> getRemboursementsByUser(String userId);

    /** Retourne un remboursement par ID (avec contrôle d'accès). */
    Remboursement getRemboursementById(String remboursementId, User user);

    // ── ADMIN ────────────────────────────────────────────────────────────────────

    /** Approuve et verse le montant (ajustable) dans le SavingsAccount. */
    Remboursement approuverRemboursement(String remboursementId, String adminId, BigDecimal montantAjuste);

    /** Refuse un remboursement EN_ATTENTE avec motif obligatoire. */
    Remboursement refuserRemboursement(String remboursementId, String motif);

    /** Liste paginée avec filtre optionnel sur le statut. */
    Page<Remboursement> getAllRemboursements(StatutRemboursement statut, Pageable pageable);

    /** Retourne tous les remboursements d'un farmer donné (vue admin). */
    List<Remboursement> getRemboursementsByUserId(String userId);

    /** Retourne les remboursements par statut (sans pagination). */
    List<Remboursement> getRemboursementsByStatut(StatutRemboursement statut);

    /** Retourne les dossiers suspects (fréquence élevée, montants anormaux). */
    List<Remboursement> getRemboursementsSuspects();

    /** Statistiques globales. */
    Map<String, Object> getRemboursementStats();
}
