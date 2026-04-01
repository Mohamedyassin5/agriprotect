package tn.esprit.agri.entities.enums;

public enum InsuranceStatus {
    PENDING_SIGNATURE,   // En attente de signature (après souscription)
    ACTIVE,              // Signée et couverture active (peut être en attente de paiement)
    PAID,                // Premier paiement effectué
    OVERDUE,             // En retard de paiement
    SUSPENDED,           // Suspendue pour non-paiement
    CANCELLED,           // Annulée par l'utilisateur ou l'admin
    COMPLETED,           // Tous les paiements terminés
    EXPIRED;             // Police arrivée à terme

    // Optionnel : méthode helper
    public boolean isCancellable() {
        return this == PENDING_SIGNATURE;
    }
}