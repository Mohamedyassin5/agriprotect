package tn.esprit.agri.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.agri.dto.PaymentResponse;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.Payment;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.entities.enums.PaymentMode;
import tn.esprit.agri.entities.enums.PaymentStatus;
import tn.esprit.agri.repositories.InsuranceRepository;
import tn.esprit.agri.repositories.PaymentRepository;
import tn.esprit.agri.services.EmailService;
import tn.esprit.agri.services.PaymentService;
import tn.esprit.agri.services.PdfService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final InsuranceRepository insuranceRepository;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    @Value("${stripe.api.key}")
    private String stripeSecretKey;
    private final PdfService pdfService;

    @Override
    @Transactional
    public PaymentResponse initiateStripePayment(String insuranceId, String userId) {
        Insurance insurance = insuranceRepository.findByIdAndUserId(insuranceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée ou accès non autorisé"));

        if (insurance.getStatus() != InsuranceStatus.ACTIVE) {
            throw new IllegalStateException("La police doit être signée avant d'effectuer un paiement");
        }

        Stripe.apiKey = stripeSecretKey;

        BigDecimal amountPerPayment = insurance.getAmountPerPayment() != null ? insurance.getAmountPerPayment() : BigDecimal.ZERO;
        BigDecimal penaltyAmount = insurance.getPenaltyAmount() != null ? insurance.getPenaltyAmount() : BigDecimal.ZERO;
        BigDecimal totalToPay = amountPerPayment.add(penaltyAmount);

        // Stripe en USD (cents)
        long amountInCents = totalToPay.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("insuranceId", insuranceId);
        metadata.put("userId", userId);
        metadata.put("policyNumber", insurance.getPolicyNumber() != null ? insurance.getPolicyNumber() : "");
        metadata.put("paymentMode", insurance.getPaymentMode() != null ? insurance.getPaymentMode().name() : "");

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setDescription("Paiement échéance - Police " + insurance.getPolicyNumber())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("PaymentIntent créé avec succès pour la police {} | Intent ID: {} | Montant: {} TND",
                    insurance.getPolicyNumber(), paymentIntent.getId(), totalToPay);

            return new PaymentResponse(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    amountPerPayment,
                    penaltyAmount,
                    totalToPay,
                    "TND",
                    insurance.getPaymentMode() != null ? insurance.getPaymentMode().name() : "UNKNOWN",
                    insurance.getPolicyNumber()
            );

        } catch (StripeException e) {
            log.error("Erreur Stripe pour l'assurance {}", insuranceId, e);
            throw new RuntimeException("Impossible d'initialiser le paiement Stripe", e);
        }
    }

    // À ajouter dans PaymentServiceImpl
    @Override
    @Transactional
    public void handleSuccessfulPayment(String paymentIntentId, String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée"));

        // Mise à jour de l'assurance (comme avant)
        insurance.setLastPaymentDate(LocalDate.now());
        insurance.setPaymentIntentId(paymentIntentId);

        if (insurance.getRemainingPayments() == null || insurance.getRemainingPayments() <= 0) {
            insurance.setRemainingPayments(insurance.getNumberOfPayments());
        }
        insurance.setRemainingPayments(insurance.getRemainingPayments() - 1);

        if (insurance.getRemainingPayments() > 0) {
            int months = getMonthsBetweenPayments(insurance.getPaymentMode());
            LocalDate newDue = insurance.getNextPaymentDue() != null
                    ? insurance.getNextPaymentDue().plusMonths(months)
                    : LocalDate.now().plusMonths(months);
            insurance.setNextPaymentDue(newDue);
            insurance.setStatus(InsuranceStatus.ACTIVE);
            insurance.setOverdue(false);
            insurance.setPenaltyAmount(BigDecimal.ZERO);
        } else {
            insurance.setNextPaymentDue(null);
            insurance.setStatus(InsuranceStatus.COMPLETED);
        }

        insuranceRepository.save(insurance);

        // === NOUVEAU : Enregistrer l'historique ===
        Payment payment = Payment.builder()
                .insurance(insurance)
                .paymentIntentId(paymentIntentId)
                .amount(insurance.getAmountPerPayment())
                .paymentDate(LocalDateTime.now())
                .status(PaymentStatus.SUCCEEDED)
                .paymentMode(insurance.getPaymentMode() != null ? insurance.getPaymentMode().name() : null)
                .penaltyAmount(insurance.getPenaltyAmount())
                .build();

        paymentRepository.save(payment);   // injection du repository

        // Envoi email
        emailService.sendPaymentConfirmationEmail(insurance);

        log.info("✅ Paiement enregistré dans l'historique pour la police {}", insurance.getPolicyNumber());
    }
    // ====================== Autres méthodes (inchangées) ======================

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

    @Override
    @Transactional
    public void applyPenaltyIfOverdue(String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée : " + insuranceId));

        if (insurance.getNextPaymentDue() == null || !LocalDate.now().isAfter(insurance.getNextPaymentDue())) {
            return;
        }

        if (insurance.isOverdue()) {
            log.info("Pénalité déjà appliquée pour la police {}", insurance.getPolicyNumber());
            return;
        }

        BigDecimal penaltyRate = new BigDecimal("0.10");
        BigDecimal penaltyAmount = insurance.getAmountPerPayment().multiply(penaltyRate);

        insurance.setPenaltyAmount(insurance.getPenaltyAmount().add(penaltyAmount));
        insurance.setOverdue(true);
        insurance.setStatus(InsuranceStatus.OVERDUE);

        insuranceRepository.save(insurance);

        try {
            emailService.sendOverdueNotification(insurance);
            log.warn("Pénalité appliquée sur la police {}", insurance.getPolicyNumber());
        } catch (Exception e) {
            log.error("Erreur envoi email pénalité", e);
        }
    }


    // Méthode utile pour régulariser une police suspendue (à appeler depuis le controller)
    @Override
    @Transactional
    public Insurance regularizePayment(String insuranceId, String userId) {
        Insurance insurance = insuranceRepository.findByIdAndUserId(insuranceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée ou vous n'avez pas les droits"));

        // Vérification de l'état
        if (insurance.getStatus() != InsuranceStatus.OVERDUE &&
                insurance.getStatus() != InsuranceStatus.SUSPENDED) {
            throw new IllegalStateException("Cette police n'est ni en retard ni suspendue");
        }

        // Réinitialisation des pénalités et retard
        insurance.setPenaltyAmount(BigDecimal.ZERO);
        insurance.setOverdue(false);

        // Réactivation de la police
        insurance.setStatus(InsuranceStatus.ACTIVE);

        // Mise à jour de la prochaine échéance (si elle est passée)
        if (insurance.getNextPaymentDue() == null || insurance.getNextPaymentDue().isBefore(LocalDate.now())) {
            int monthsInterval = getMonthsBetweenPayments(insurance.getPaymentMode());
            insurance.setNextPaymentDue(LocalDate.now().plusMonths(monthsInterval));
        }

        // Sauvegarde
        Insurance savedInsurance = insuranceRepository.save(insurance);

        // Envoi d'un email de confirmation de régularisation
        try {
            emailService.sendRegularizationConfirmationEmail(savedInsurance);
            log.info("Email de régularisation envoyé pour la police {}", savedInsurance.getPolicyNumber());
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'email de régularisation", e);
        }

        return savedInsurance;
    }
    @Override
    @Transactional
    public PaymentResponse initiateRegularizationPayment(String insuranceId, String userId) {
        Insurance insurance = insuranceRepository.findByIdAndUserId(insuranceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée ou accès non autorisé"));

        if (insurance.getStatus() != InsuranceStatus.OVERDUE &&
                insurance.getStatus() != InsuranceStatus.SUSPENDED) {
            throw new IllegalStateException("Cette police n'est pas en retard ou suspendue");
        }

        Stripe.apiKey = stripeSecretKey;

        BigDecimal amountPerPayment = insurance.getAmountPerPayment() != null ? insurance.getAmountPerPayment() : BigDecimal.ZERO;
        BigDecimal penaltyAmount = insurance.getPenaltyAmount() != null ? insurance.getPenaltyAmount() : BigDecimal.ZERO;
        BigDecimal totalArrears = amountPerPayment.add(penaltyAmount);

        // USD = 2 décimales → multiplier par 100
        long amountInCents = totalArrears.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("insuranceId", insuranceId);
        metadata.put("userId", userId);
        metadata.put("paymentType", "REGULARIZATION");
        metadata.put("policyNumber", insurance.getPolicyNumber() != null ? insurance.getPolicyNumber() : "");

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")                    // USD pour le moment
                    .setDescription("Régularisation police " + insurance.getPolicyNumber())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("PaymentIntent de régularisation créé pour {} | Montant: {} TND (facturé en USD) | Intent: {}",
                    insurance.getPolicyNumber(), totalArrears, paymentIntent.getId());

            return new PaymentResponse(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    amountPerPayment,
                    penaltyAmount,
                    totalArrears,
                    "TND",                    // On affiche en TND pour l'utilisateur
                    "REGULARIZATION",
                    insurance.getPolicyNumber()
            );

        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la régularisation de l'assurance {}", insuranceId, e);
            throw new RuntimeException("Impossible d'initialiser le paiement de régularisation", e);
        }
    }
    @Override
    @Transactional
    public void handleRegularizationSuccess(String paymentIntentId, String insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new IllegalArgumentException("Police non trouvée : " + insuranceId));

        // Réinitialisation complète de la police
        insurance.setLastPaymentDate(LocalDate.now());
        insurance.setPaymentIntentId(paymentIntentId);
        insurance.setPenaltyAmount(BigDecimal.ZERO);
        insurance.setOverdue(false);
        insurance.setStatus(InsuranceStatus.ACTIVE);

        // Mise à jour de la prochaine échéance
        if (insurance.getNextPaymentDue() == null || insurance.getNextPaymentDue().isBefore(LocalDate.now())) {
            int monthsInterval = getMonthsBetweenPayments(insurance.getPaymentMode());
            insurance.setNextPaymentDue(LocalDate.now().plusMonths(monthsInterval));
        }

        // Réinitialiser le nombre de paiements restants si nécessaire
        if (insurance.getRemainingPayments() == null || insurance.getRemainingPayments() <= 0) {
            insurance.setRemainingPayments(insurance.getNumberOfPayments());
        }

        insuranceRepository.save(insurance);

        // Envoi de l'email spécifique de régularisation
        try {
            emailService.sendRegularizationConfirmationEmail(insurance);
            log.info("✅ RÉGULARISATION réussie - Police: {} | Statut: ACTIVE | Prochain paiement: {}",
                    insurance.getPolicyNumber(), insurance.getNextPaymentDue());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de régularisation", e);
        }
    }
    @Override
    public byte[] generateInvoicePdf(String insuranceId, String userId) {
        // Vérification que la police existe et appartient à l'utilisateur
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Police non trouvée"));

        if (!insurance.getUser().getId().equals(userId)) {
            log.warn("Tentative d'accès non autorisé à la facture de la police {} par l'utilisateur {}", insuranceId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette police ne vous appartient pas");
        }

        // Vérification que la police a au moins un paiement
        List<Payment> payments = paymentRepository.findByInsuranceOrderByPaymentDateDesc(insurance);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun paiement trouvé pour cette police");
        }

        try {
            // Ici tu appelles ton service de génération de PDF (PdfService, iText, etc.)
            return pdfService.generateInvoice(insurance, payments.get(0));   // ou tous les paiements

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la facture pour la police {}", insuranceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de générer la facture PDF");
        }
    }
}