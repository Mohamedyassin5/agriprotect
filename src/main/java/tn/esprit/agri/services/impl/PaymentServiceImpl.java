package tn.esprit.agri.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.agri.dto.PaymentResponse;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.entities.enums.PaymentMode;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.services.EmailService;
import tn.esprit.agri.services.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final InsuranceRepository insuranceRepository;
    private final EmailService emailService;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Override
    @Transactional
    public PaymentResponse initiateStripePayment(String insuranceId, String userId) {

        Insurance insurance = insuranceRepository.findByIdAndUserId(insuranceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée ou accès non autorisé"));

        if (insurance.getStatus() != InsuranceStatus.ACTIVE) {
            throw new IllegalStateException("La police doit être signée avant d'effectuer un paiement");
        }

        // Configuration Stripe
        Stripe.apiKey = stripeSecretKey;

        // Calcul du montant en centimes
        BigDecimal amountToPay = insurance.getAmountPerPayment();
        long amountInCents = amountToPay.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("insuranceId", insuranceId);
        metadata.put("userId", userId);
        metadata.put("policyNumber", insurance.getPolicyNumber());
        metadata.put("paymentMode", insurance.getPaymentMode().name());

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")                    // Change en "tnd" si Stripe le supporte en live
                    .setDescription("Paiement échéance - Police " + insurance.getPolicyNumber())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("PaymentIntent créé avec succès pour la police {} | Intent ID: {}",
                    insurance.getPolicyNumber(), paymentIntent.getId());

            // Retour avec toutes les informations nécessaires pour le frontend
            return new PaymentResponse(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    amountToPay,                    // montant en TND (ex: 125.50)
                    "USD",                          // ou "TND"
                    insurance.getPaymentMode().name(),
                    insurance.getPolicyNumber()
            );

        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création du PaymentIntent pour l'assurance {}", insuranceId, e);
            throw new RuntimeException("Impossible d'initialiser le paiement Stripe", e);
        }
    }

    @Override
    @Transactional
    public void handleSuccessfulPayment(String paymentIntentId, String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée : " + insuranceId));

        // Mise à jour des informations de paiement
        insurance.setLastPaymentDate(LocalDate.now());
        insurance.setPaymentIntentId(paymentIntentId);

        // Initialisation du compteur si c'est le premier paiement
        if (insurance.getRemainingPayments() == null || insurance.getRemainingPayments() <= 0) {
            insurance.setRemainingPayments(insurance.getNumberOfPayments());
        }

        // Décompte d'une mensualité
        insurance.setRemainingPayments(insurance.getRemainingPayments() - 1);

        // Calcul de la prochaine échéance
        if (insurance.getRemainingPayments() > 0) {
            int monthsInterval = getMonthsBetweenPayments(insurance.getPaymentMode());
            LocalDate newNextDue = (insurance.getNextPaymentDue() != null)
                    ? insurance.getNextPaymentDue().plusMonths(monthsInterval)
                    : LocalDate.now().plusMonths(monthsInterval);

            insurance.setNextPaymentDue(newNextDue);
            insurance.setStatus(InsuranceStatus.ACTIVE);
        } else {
            insurance.setNextPaymentDue(null);
            insurance.setStatus(InsuranceStatus.COMPLETED);
        }

        insuranceRepository.save(insurance);

        // Envoi de l'email de confirmation
        try {
            emailService.sendPaymentConfirmationEmail(insurance);
            log.info("✅ Paiement traité avec succès - Police: {} | Restant: {} | Prochain: {}",
                    insurance.getPolicyNumber(),
                    insurance.getRemainingPayments(),
                    insurance.getNextPaymentDue());
        } catch (Exception e) {
            log.error("Erreur envoi email confirmation paiement", e);
        }
    }
    @Override
    public String getPaymentStatus(String paymentIntentId) {
        Stripe.apiKey = stripeSecretKey;
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.getStatus();
        } catch (StripeException e) {
            log.error("Erreur lors de la récupération du statut du PaymentIntent {}", paymentIntentId, e);
            return "error";
        }
    }

    private int getMonthsBetweenPayments(PaymentMode mode) {
        return switch (mode) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case SEMI_ANNUAL -> 6;
            case ANNUAL -> 12;
        };
    }
}