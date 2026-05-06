package tn.esprit.agri.services;

import tn.esprit.agri.DTO.InvoiceDTO;
import tn.esprit.agri.DTO.PaymentResponse;
import tn.esprit.agri.DTO.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;

public interface PaymentService {

    /**
     * Crée un PaymentIntent Stripe pour le premier paiement selon le mode choisi
     */
    PaymentResponse initiateStripePayment(String insuranceId, String userId);

    /**
     * Méthode à appeler via Webhook Stripe quand le paiement est réussi
     */
    void handleSuccessfulPayment(String paymentIntentId, String insuranceId);

    /**
     * Récupère les détails d'un PaymentIntent (optionnel)
     */
    String getPaymentStatus(String paymentIntentId);
    void applyPenaltyIfOverdue(String insuranceId);
    /**
     * Régularise une police suspendue ou en retard :
     * - Réinitialise la pénalité
     * - Réactive la police
     * - Met à jour la prochaine échéance
     */
    Insurance regularizePayment(String insuranceId, String userId);
    /**
     * Calcule le montant total des arriérés et initie un paiement Stripe pour régulariser une police suspendue
     */
    PaymentResponse initiateRegularizationPayment(String insuranceId, String userId);

    /**
     * Gère le succès d'un paiement de régularisation (réactive la police)
     */
    void handleRegularizationSuccess(String paymentIntentId, String insuranceId);
    /**
     * Génère la facture PDF pour une police d'assurance
     * @param insuranceId ID de la police
     * @param userId ID de l'utilisateur (pour vérification de propriété)
     * @return Tableau de bytes du PDF
     */
    byte[] generateInvoicePdf(String insuranceId, String userId);
    InvoiceDTO getInvoiceData(String insuranceId, String userId);
}
