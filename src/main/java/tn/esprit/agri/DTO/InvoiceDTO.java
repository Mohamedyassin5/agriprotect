package tn.esprit.agri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO enrichi pour la génération de facture / quittance AgriProtect.
 * Contient toutes les informations nécessaires pour l'affichage côté frontend
 * et la génération PDF côté backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {

    // ── Identifiants ──────────────────────────────────────────────────────────
    private String invoiceNumber;          // ex: FAC-2025-00042
    private String policyNumber;           // N° police
    private String insuranceId;

    // ── Assuré ────────────────────────────────────────────────────────────────
    private String farmerName;
    private String farmerEmail;
    private String farmerPhone;
    private String farmerAddress;

    // ── Police ────────────────────────────────────────────────────────────────
    private String coverageType;           // BASIC / STANDARD / PREMIUM / COMPREHENSIVE
    private String status;                 // statut actuel
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private BigDecimal insuredAmount;      // montant assuré total
    private String paymentMode;            // MONTHLY / QUARTERLY / SEMI_ANNUAL / ANNUAL

    // ── Paiement courant ──────────────────────────────────────────────────────
    private BigDecimal amountPerPayment;   // prime par échéance
    private BigDecimal penaltyAmount;      // pénalité éventuelle
    private BigDecimal totalDue;           // amountPerPayment + penaltyAmount
    private LocalDateTime paymentDate;     // date du paiement effectué
    private String paymentIntentId;        // référence Stripe
    private String paymentStatus;          // SUCCEEDED / FAILED / REFUNDED

    // ── Suivi financier ───────────────────────────────────────────────────────
    private BigDecimal annualPremium;      // prime annuelle totale
    private BigDecimal totalPaid;          // total déjà payé (somme historique)
    private BigDecimal remainingBalance;   // ce qu'il reste à payer
    private int totalPayments;             // nombre total d'échéances
    private int paymentsCompleted;         // échéances déjà réglées
    private int remainingPayments;         // échéances restantes
    private LocalDate nextPaymentDue;      // prochaine échéance

    // ── Historique (résumé) ───────────────────────────────────────────────────
    private List<PaymentSummary> paymentHistory;

    // ── Dates facture ─────────────────────────────────────────────────────────
    private LocalDate invoiceDate;         // date d'émission de la facture

    // ─────────────────────────────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private LocalDateTime date;
        private BigDecimal amount;
        private BigDecimal penalty;
        private String status;
        private String mode;
        private String intentId;
    }
}
