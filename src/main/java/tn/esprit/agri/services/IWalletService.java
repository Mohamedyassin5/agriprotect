package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.DTO.wallet.*;

import java.util.List;

public interface IWalletService {
    WalletResponse getOrCreateWallet(String userId);
    WalletResponse deposit(String userId, WalletDepositRequest request);
    WalletResponse withdraw(String userId, WalletDepositRequest request);
    WalletResponse transfer(String userId, WalletTransferRequest request);
    WalletResponse configureEmergencyFund(String userId, EmergencyConfigRequest request);
    WalletResponse topUpEmergencyFund(String userId, EmergencyFundTopUpRequest request);
    EmergencyWithdrawalResponse requestEmergencyWithdrawal(String userId, EmergencyWithdrawalRequest request);
    Page<WalletTransactionResponse> getTransactions(String userId, Pageable pageable);
    List<EmergencyWithdrawalResponse> getEmergencyWithdrawals(String userId);

    // Admin
    WalletAdminOverview getAdminOverview();
    List<EmergencyWithdrawalResponse> getPendingWithdrawals();
    EmergencyWithdrawalResponse approveWithdrawal(Long withdrawalId, String adminId, String note);
    EmergencyWithdrawalResponse rejectWithdrawal(Long withdrawalId, String adminId, String note);
}
