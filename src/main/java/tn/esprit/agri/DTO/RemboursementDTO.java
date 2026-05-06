package tn.esprit.agri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.StatutRemboursement;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemboursementDTO {

    // ── Identifiants ─────────────────────────────────────────────────────────────
    private String remboursementId;   // ID en base (null si simulation)
    private String sinistreId;
    private String insuranceId;
    private String policyNumber;

    // ── Détails du calcul ────────────────────────────────────────────────────────
    private BigDecimal montantDommagesDeclares;
    private BigDecimal montantFranchise;
    private BigDecimal montantRemboursableAvantRegles;
    private BigDecimal coefficientProrata;
    private BigDecimal montantApresProrata;
    private BigDecimal primesRestantesDues;
    private BigDecimal penaliteResiliation;
    private BigDecimal montantFinalRembourse;


    // ── Infos contrat ────────────────────────────────────────────────────────────
    private int moisPayes;
    private int moisRestants;
    private BigDecimal primeParMois;

    // ── Statut et dates ──────────────────────────────────────────────────────────
    // statut = null si c'est une simulation (dry-run)
    private StatutRemboursement statut;
    private LocalDate dateRemboursement;   // null si EN_ATTENTE (paiement pas encore effectué)

    // ── Traçabilité admin ────────────────────────────────────────────────────────
    private String approuveParAdminId;     // ID de l'admin ayant validé (null si pas encore approuvé)
    private String motifRefus;             // Renseigné uniquement si REFUSE

    // ── Message d'information ─────────────────────────────────────────────────────
    // Peut contenir un avertissement (plafonnement, déductions) ou un message de simulation
    private String avertissement;

    // ── Indicateur de simulation ─────────────────────────────────────────────────
    // true = dry-run (aucune persistance), false = demande réelle
    @Builder.Default
    private boolean simulation = false;
}
