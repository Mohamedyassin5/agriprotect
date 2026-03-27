package tn.esprit.agri.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
public class PaymentResponse {
    private String paymentIntentId;
    private String clientSecret;
    private BigDecimal amount;           // montant en TND (ex: 125.50)
    private String currency;             // "tnd" ou "usd"
    private String paymentMode;
    private String policyNumber;
}