package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.agri.entities.enums.StatutRemboursement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité Remboursement — ajouter le champ stripeRefundId
 * (les autres champs restent identiques à votre version actuelle)
 *
 * ✅ SEUL CHANGEMENT : ajout de stripeRefundId
 */
@Entity
@Table(name = "remboursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remboursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "insurance_id", nullable = false)
    private Insurance insurance;

    @ManyToOne
    @JoinColumn(name = "sinistre_id", nullable = false)
    private Sinistre sinistre;

    // ── Détails du calcul ────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 2)
    private BigDecimal montantDommagesDeclares;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantFranchise;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantRemboursableAvantRegles;

    @Column(precision = 8, scale = 4)
    private BigDecimal coefficientProrata;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantApresProrata;

    @Column(precision = 12, scale = 2)
    private BigDecimal primesRestantesDues;

    @Column(precision = 12, scale = 2)
    private BigDecimal penaliteResiliation;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantFinalRembourse;

    // ── Statut ───────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRemboursement statut;

    private LocalDate dateRemboursement;

    @Column(length = 500)
    private String motifRefus;
    @Column(name = "approuve_par_admin_id", length = 255)
    private String approuveParAdminId;
    @Column(length = 1000)
    private String avertissement;
    // ── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // Remboursement.java
    @Column(name = "mois_restants")
    private Integer moisRestants;

    @Column(name = "prime_par_mois")
    private BigDecimal primeParMois;
    @Column(name = "stripe_refund_id")
    private String stripeRefundId;
}
