package tn.esprit.agri.controlleurs;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.Remboursement;
import tn.esprit.agri.entities.enums.StatutRemboursement;
import tn.esprit.agri.repositories.RemboursementRepository;
import tn.esprit.agri.services.PaymentService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final RemboursementRepository remboursementRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // Anti-doublons
    private static final Set<String> processedPaymentIntents = ConcurrentHashMap.newKeySet();
    private static final Set<String> processedRefunds        = ConcurrentHashMap.newKeySet();

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
            log.info("✅ Webhook Stripe reçu : {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("❌ Signature webhook invalide", e);
            return ResponseEntity.badRequest().body("Signature invalide");
        } catch (Exception e) {
            log.error("❌ Erreur parsing webhook", e);
            return ResponseEntity.badRequest().body("Payload invalide");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded"       -> handlePaymentSuccess(event);
            case "payment_intent.payment_failed"  -> log.warn("❌ Paiement échoué : {}", event.getId());

            // ✅ NOUVEAU — Remboursement Stripe confirmé → statut PAYE garanti
            case "charge.refunded"                -> handleRefundConfirmed(event);
            case "refund.failed"                  -> handleRefundFailed(event);

            default -> log.debug("Événement Stripe ignoré : {}", event.getType());
        }

        return ResponseEntity.ok("Webhook received");
    }

    // ── Paiement de prime ────────────────────────────────────────────────────────

    private void handlePaymentSuccess(Event event) {
        try {
            String json = event.getDataObjectDeserializer().getRawJson();
            if (json == null) { log.error("JSON PaymentIntent null"); return; }

            PaymentIntent paymentIntent = PaymentIntent.GSON.fromJson(json, PaymentIntent.class);
            if (paymentIntent == null) return;

            String paymentIntentId = paymentIntent.getId();
            String insuranceId     = paymentIntent.getMetadata().get("insuranceId");
            String paymentType     = paymentIntent.getMetadata().get("paymentType");

            if (!processedPaymentIntents.add(paymentIntentId)) {
                log.info("PaymentIntent {} déjà traité", paymentIntentId);
                return;
            }

            if (insuranceId == null || insuranceId.isBlank()) {
                log.warn("insuranceId manquant dans les metadata");
                return;
            }

            if ("REGULARIZATION".equalsIgnoreCase(paymentType)) {
                paymentService.handleRegularizationSuccess(paymentIntentId, insuranceId);
            } else {
                paymentService.handleSuccessfulPayment(paymentIntentId, insuranceId);
            }

        } catch (Exception e) {
            log.error("💥 Erreur traitement payment_intent.succeeded", e);
        }
    }

    // ── Remboursement sinistre confirmé par Stripe ────────────────────────────────

    /**
     * Stripe envoie "charge.refunded" quand le remboursement est confirmé côté Stripe.
     * On met à jour le statut en PAYE pour s'assurer de la cohérence même si
     * le service l'a déjà passé en PAYE lors de la création du Refund.
     *
     * C'est une sécurité supplémentaire (double confirmation).
     */
    private void handleRefundConfirmed(Event event) {
        try {
            String json = event.getDataObjectDeserializer().getRawJson();
            if (json == null) { log.error("JSON Charge null pour refund"); return; }

            // Extraire le refund_id depuis la charge
            // La charge contient le refund dans charge.refunds.data[0].id
            com.stripe.model.Charge charge = com.stripe.model.Charge.GSON.fromJson(
                    json, com.stripe.model.Charge.class);
            if (charge == null) return;

            String refundId = null;
            if (charge.getRefunds() != null
                    && charge.getRefunds().getData() != null
                    && !charge.getRefunds().getData().isEmpty()) {
                refundId = charge.getRefunds().getData().get(0).getId();
            }

            if (refundId == null) {
                log.warn("Aucun refundId trouvé dans charge.refunded");
                return;
            }

            if (!processedRefunds.add(refundId)) {
                log.info("Refund {} déjà traité", refundId);
                return;
            }

            final String finalRefundId = refundId;
            Optional<Remboursement> opt = remboursementRepository.findByStripeRefundId(finalRefundId);
            if (opt.isPresent()) {
                Remboursement r = opt.get();
                if (r.getStatut() != StatutRemboursement.PAYE) {
                    r.setStatut(StatutRemboursement.PAYE);
                    r.setDateRemboursement(LocalDate.now());
                    remboursementRepository.save(r);
                    log.info("✅ Remboursement {} confirmé PAYE via webhook charge.refunded", r.getId());
                }
            } else {
                log.warn("Aucun remboursement trouvé pour stripeRefundId={}", finalRefundId);
            }

        } catch (Exception e) {
            log.error("💥 Erreur traitement charge.refunded", e);
        }
    }

    /**
     * Si le refund Stripe échoue après la création, on repasse le remboursement en EN_ATTENTE
     * pour qu'un admin puisse intervenir.
     */
    private void handleRefundFailed(Event event) {
        try {
            String json = event.getDataObjectDeserializer().getRawJson();
            if (json == null) return;

            Refund refund = Refund.GSON.fromJson(json, Refund.class);
            if (refund == null) return;

            String refundId = refund.getId();
            remboursementRepository.findByStripeRefundId(refundId).ifPresent(r -> {
                r.setStatut(StatutRemboursement.EN_ATTENTE);
                r.setAvertissement("⚠️ Échec du remboursement Stripe (refund.failed). Intervention admin requise.");
                remboursementRepository.save(r);
                log.error("❌ Refund Stripe échoué pour remboursement {} | refundId={}", r.getId(), refundId);
            });

        } catch (Exception e) {
            log.error("💥 Erreur traitement refund.failed", e);
        }
    }
}
