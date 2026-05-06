package tn.esprit.agri.DTO.wallet;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class WalletAdminOverview {
    private long totalWallets;
    private BigDecimal totalPlatformBalance;
    private BigDecimal totalEmergencyFunds;
    private long farmersWithNoEmergencyFund;
    private long pendingWithdrawals;
}
