package tn.esprit.agri.controlleurs;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.services.PaymentService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // Anti-doublons (très bonne pratique)
    private static final Set<String> processedPaymentIntents = ConcurrentHashMap.newKeySet();

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

        // On traite seulement les événements de succès
        if ("payment_intent.succeeded".equals(event.getType())) {
            handlePaymentSuccess(event);
        }
        // Tu peux ajouter d'autres événements plus tard (payment_intent.payment_failed, etc.)
        else if ("payment_intent.payment_failed".equals(event.getType())) {
            log.warn("❌ Paiement échoué : {}", event.getId());
            // Optionnel : notifier l'utilisateur
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handlePaymentSuccess(Event event) {
        try {
            // Extraction robuste du PaymentIntent
            String json = event.getDataObjectDeserializer().getRawJson();
            if (json == null) {
                log.error("Impossible de récupérer le JSON du PaymentIntent");
                return;
            }

            PaymentIntent paymentIntent = PaymentIntent.GSON.fromJson(json, PaymentIntent.class);
            if (paymentIntent == null) return;

            String paymentIntentId = paymentIntent.getId();
            String insuranceId = paymentIntent.getMetadata().get("insuranceId");
            String paymentType = paymentIntent.getMetadata().get("paymentType");

            // Protection anti-doublons
            if (!processedPaymentIntents.add(paymentIntentId)) {
                log.info("PaymentIntent {} déjà traité", paymentIntentId);
                return;
            }

            log.info("📦 Paiement réussi → Type: '{}' | InsuranceId: '{}' | Intent: {}",
                    paymentType, insuranceId, paymentIntentId);

            if (insuranceId == null || insuranceId.isBlank()) {
                log.warn("insuranceId manquant dans les metadata du PaymentIntent");
                return;
            }

            // === DISTINCTION SELON LE TYPE ===
            if ("REGULARIZATION".equalsIgnoreCase(paymentType)) {
                log.info("🔄 Traitement de la RÉGULARISATION pour la police {}", insuranceId);
                paymentService.handleRegularizationSuccess(paymentIntentId, insuranceId);
            }
            else {
                // Paiement normal (échéance mensuelle, etc.)
                log.info("💰 Traitement du paiement NORMAL pour la police {}", insuranceId);
                paymentService.handleSuccessfulPayment(paymentIntentId, insuranceId);
            }

        } catch (Exception e) {
            log.error("💥 Erreur critique lors du traitement du webhook payment_intent.succeeded", e);
        }
    }
}