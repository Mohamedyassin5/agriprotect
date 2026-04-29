package tn.esprit.agri.services.impl;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    public PaymentIntent createPaymentIntent(Double amount, String currency, String description) throws Exception {
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        
        // Ensure API key is set before call
        Stripe.apiKey = stripeApiKey;
        
        System.out.println("DEBUG: Creating Stripe PaymentIntent for " + amount + " " + currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // Stripe expects cents/smallest unit
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }
}
