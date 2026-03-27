package tn.esprit.agri.services;

import tn.esprit.agri.dto.PaymentResponse;
import tn.esprit.agri.dto.SignRequestDTO;
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
}
