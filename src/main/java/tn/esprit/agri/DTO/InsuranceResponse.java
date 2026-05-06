package tn.esprit.agri.DTO;

import lombok.Data;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.InsuranceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InsuranceResponse {
    private String id;
    private String policyNumber;
    private CoverageType coverageType;
    private BigDecimal insuredAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private InsuranceStatus status;
    private String message;

    private String paymentMode;           // ex: "MONTHLY", "QUARTERLY"
    private BigDecimal totalPremium;
    private BigDecimal amountPerPayment;
    private Integer numberOfPayments;
    private Integer remainingPayments;
    private LocalDate nextPaymentDue;
    private BigDecimal penaltyAmount;
    private boolean overdue;
    private String signedByName;
    private String signedAt;
    private String signatureImage;
    private Integer unpaidMonths;   // nombre de mois impayés calculé
    private LocalDate suspendedAt;  // date de suspension
    public String getSignedAt() { return signedAt; }
    public void setSignedAt(String signedAt) { this.signedAt = signedAt; }// base64 string pour le frontend
}