package tn.esprit.agri.controlleurs;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.ApiResource;
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

    // On garde en mémoire les PaymentIntent déjà traités pour éviter les doublons
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
            return ResponseEntity.status(400).body("Signature invalide");
        } catch (Exception e) {
            log.error("❌ Erreur parsing webhook", e);
            return ResponseEntity.status(400).body("Payload invalide");
        }

        // On ne traite que les événements de succès
        if ("payment_intent.succeeded".equals(event.getType()) ||
                "charge.succeeded".equals(event.getType())) {

            handlePaymentSuccess(event);
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handlePaymentSuccess(Event event) {
        try {
            var deserializer = event.getDataObjectDeserializer();
            PaymentIntent paymentIntent = null;

            if (deserializer.getObject().isPresent()) {
                Object obj = deserializer.getObject().get();
                if (obj instanceof PaymentIntent) {
                    paymentIntent = (PaymentIntent) obj;
                }
            } else {
                String json = deserializer.getRawJson();
                paymentIntent = ApiResource.GSON.fromJson(json, PaymentIntent.class);
            }

            if (paymentIntent == null) return;

            String paymentIntentId = paymentIntent.getId();
            String insuranceId = paymentIntent.getMetadata().get("insuranceId");

            // === PROTECTION CONTRE LES DOUBLONS ===
            if (!processedPaymentIntents.add(paymentIntentId)) {
                log.info("⚠️ PaymentIntent {} déjà traité, on ignore", paymentIntentId);
                return;
            }

            log.info("📦 Traitement du paiement - PaymentIntent: {} | insuranceId: {}",
                    paymentIntentId, insuranceId);

            if (insuranceId != null) {
                paymentService.handleSuccessfulPayment(paymentIntentId, insuranceId);
                log.info("✅ PAIEMENT TRAITÉ AVEC SUCCÈS pour insuranceId: {}", insuranceId);
            }

        } catch (Exception e) {
            log.error("💥 ERREUR lors du traitement du paiement réussi", e);
        }
    }
}