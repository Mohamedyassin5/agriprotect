package tn.esprit.agri.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient funds. Current balance: %s, Requested amount: %s",
              currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
