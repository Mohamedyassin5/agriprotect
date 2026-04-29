package tn.esprit.agri.controlleurs;

import com.stripe.model.PaymentIntent;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.services.impl.StripeService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final StripeService stripeService;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequest request) {
        try {
            // Using EUR for better Stripe Test compatibility
            PaymentIntent intent = stripeService.createPaymentIntent(
                    request.getAmount(),
                    "eur",
                    "Paiement Prime AgriProtect - " + request.getFundName()
            );

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur Stripe: " + e.getMessage());
        }
    }

    @Data
    public static class PaymentRequest {
        private Double amount;
        private String fundId;
        private String fundName;
    }
}
