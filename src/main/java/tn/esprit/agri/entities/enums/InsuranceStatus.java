package tn.esprit.agri.entities.enums;

public enum InsuranceStatus {

    PENDING_SIGNATURE,   // En attente de signature
    ACTIVE,              // Signée mais pas encore payée
    PAID,                // Premier paiement effectué (couverture active)
    COMPLETED,           // Tous les paiements terminés
    CANCELLED,           // Annulée
    EXPIRED;             // Expirée

}