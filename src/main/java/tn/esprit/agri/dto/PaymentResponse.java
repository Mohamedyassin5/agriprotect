package tn.esprit.agri.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;


// PaymentResponse.java
@Data
@AllArgsConstructor
public class PaymentResponse {
    private String paymentIntentId;
    private String clientSecret;
    private BigDecimal baseAmount;        // montant de base
    private BigDecimal penaltyAmount;     // pénalité
    private BigDecimal totalAmount;       // ← NOUVEAU : montant total à payer
    private String currency;
    private String paymentMode;
    private String policyNumber;
}