package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.wallet.*;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.entities.enums.*;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.IWalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements IWalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final EmergencyWithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;

    @Override
    public WalletResponse getOrCreateWallet(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return walletRepository.save(Wallet.builder().user(user).build());
        });
        return toResponse(wallet);
    }

    @Override
    public WalletResponse deposit(String userId, WalletDepositRequest req) {
        Wallet wallet = getWallet(userId);
        BigDecimal before = wallet.getAvailableBalance();
        wallet.setAvailableBalance(before.add(req.getAmount()));
        recordTx(wallet, WalletTransactionType.DEPOSIT, req.getAmount(), before,
                wallet.getAvailableBalance(), req.getDescription(), null, null);
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public WalletResponse withdraw(String userId, WalletDepositRequest req) {
        Wallet wallet = getWallet(userId);
        if (wallet.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Solde insuffisant");
        BigDecimal before = wallet.getAvailableBalance();
        wallet.setAvailableBalance(before.subtract(req.getAmount()));
        recordTx(wallet, WalletTransactionType.WITHDRAWAL, req.getAmount(), before,
                wallet.getAvailableBalance(), req.getDescription(), null, null);
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public WalletResponse transfer(String userId, WalletTransferRequest req) {
        Wallet sender = getWallet(userId);
        Wallet recipient = getWallet(req.getRecipientUserId());
        if (sender.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Solde insuffisant");
        BigDecimal senderBefore = sender.getAvailableBalance();
        sender.setAvailableBalance(senderBefore.subtract(req.getAmount()));
        recordTx(sender, WalletTransactionType.PEER_TRANSFER_OUT, req.getAmount(),
                senderBefore, sender.getAvailableBalance(), req.getDescription(), null, null);
        walletRepository.save(sender);

        BigDecimal recipientBefore = recipient.getAvailableBalance();
        recipient.setAvailableBalance(recipientBefore.add(req.getAmount()));
        recordTx(recipient, WalletTransactionType.PEER_TRANSFER_IN, req.getAmount(),
                recipientBefore, recipient.getAvailableBalance(), req.getDescription(), null, null);
        walletRepository.save(recipient);

        return toResponse(sender);
    }

    @Override
    public WalletResponse configureEmergencyFund(String userId, EmergencyConfigRequest req) {
        Wallet wallet = getWallet(userId);
        wallet.setEmergencyTargetAmount(req.getTargetAmount());
        wallet.setEmergencyMonthlyContribution(req.getMonthlyContribution());
        wallet.setEmergencyAutoContribute(req.isAutoContribute());
        if (req.getContributionDay() != null) wallet.setEmergencyContributionDay(req.getContributionDay());
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public WalletResponse topUpEmergencyFund(String userId, EmergencyFundTopUpRequest req) {
        Wallet wallet = getWallet(userId);
        if (wallet.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Solde insuffisant");
        BigDecimal before = wallet.getAvailableBalance();
        wallet.setAvailableBalance(before.subtract(req.getAmount()));
        BigDecimal efBefore = wallet.getEmergencyFundBalance();
        wallet.setEmergencyFundBalance(efBefore.add(req.getAmount()));
        recordTx(wallet, WalletTransactionType.TRANSFER_TO_EMERGENCY, req.getAmount(),
                before, wallet.getAvailableBalance(), "Alimentation fonds d'urgence", null, null);
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public EmergencyWithdrawalResponse requestEmergencyWithdrawal(String userId, EmergencyWithdrawalRequest req) {
        Wallet wallet = getWallet(userId);
        if (wallet.getEmergencyFundBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Fonds d'urgence insuffisant");

        EmergencyWithdrawalStatus status;
        BigDecimal threshold = wallet.getEmergencyFundBalance()
                .multiply(BigDecimal.valueOf(0.20));
        if (req.getAmount().compareTo(threshold) < 0) {
            // Auto-approve small withdrawals (< 20% of fund)
            status = EmergencyWithdrawalStatus.AUTO_APPROVED;
            BigDecimal before = wallet.getEmergencyFundBalance();
            wallet.setEmergencyFundBalance(before.subtract(req.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(req.getAmount()));
            recordTx(wallet, WalletTransactionType.TRANSFER_FROM_EMERGENCY, req.getAmount(),
                    before, wallet.getEmergencyFundBalance(), "Retrait urgence: " + req.getReason(), null, null);
            walletRepository.save(wallet);
        } else {
            status = EmergencyWithdrawalStatus.PENDING_APPROVAL;
        }

        EmergencyWithdrawal withdrawal = EmergencyWithdrawal.builder()
                .wallet(wallet)
                .amount(req.getAmount())
                .reason(req.getReason())
                .description(req.getDescription())
                .supportingDocumentUrl(req.getSupportingDocumentUrl())
                .status(status)
                .build();
        return toWithdrawalResponse(withdrawalRepository.save(withdrawal));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(String userId, Pageable pageable) {
        Wallet wallet = getWallet(userId);
        return txRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::toTxResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyWithdrawalResponse> getEmergencyWithdrawals(String userId) {
        Wallet wallet = getWallet(userId);
        return withdrawalRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream().map(this::toWithdrawalResponse).collect(Collectors.toList());
    }

    // ── Admin ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public WalletAdminOverview getAdminOverview() {
        return WalletAdminOverview.builder()
                .totalWallets(walletRepository.count())
                .totalPlatformBalance(walletRepository.totalPlatformWalletBalance())
                .totalEmergencyFunds(walletRepository.totalEmergencyFunds())
                .farmersWithNoEmergencyFund(walletRepository.countWithNoEmergencyFund())
                .pendingWithdrawals(withdrawalRepository.findByStatus(EmergencyWithdrawalStatus.PENDING_APPROVAL).size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyWithdrawalResponse> getPendingWithdrawals() {
        return withdrawalRepository.findByStatus(EmergencyWithdrawalStatus.PENDING_APPROVAL)
                .stream().map(this::toWithdrawalResponse).collect(Collectors.toList());
    }

    @Override
    public EmergencyWithdrawalResponse approveWithdrawal(Long withdrawalId, String adminId, String note) {
        EmergencyWithdrawal w = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
        if (w.getStatus() != EmergencyWithdrawalStatus.PENDING_APPROVAL)
            throw new RuntimeException("Withdrawal is not pending");
        Wallet wallet = w.getWallet();
        if (wallet.getEmergencyFundBalance().compareTo(w.getAmount()) < 0)
            throw new RuntimeException("Fonds insuffisants");
        BigDecimal before = wallet.getEmergencyFundBalance();
        wallet.setEmergencyFundBalance(before.subtract(w.getAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(w.getAmount()));
        recordTx(wallet, WalletTransactionType.TRANSFER_FROM_EMERGENCY, w.getAmount(),
                before, wallet.getEmergencyFundBalance(), "Retrait urgence approuvé: " + w.getReason(), null, null);
        walletRepository.save(wallet);
        w.setStatus(EmergencyWithdrawalStatus.APPROVED);
        w.setAdminNote(note);
        w.setApprovedBy(adminId);
        return toWithdrawalResponse(withdrawalRepository.save(w));
    }

    @Override
    public EmergencyWithdrawalResponse rejectWithdrawal(Long withdrawalId, String adminId, String note) {
        EmergencyWithdrawal w = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
        w.setStatus(EmergencyWithdrawalStatus.REJECTED);
        w.setAdminNote(note);
        w.setApprovedBy(adminId);
        return toWithdrawalResponse(withdrawalRepository.save(w));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Wallet getWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return walletRepository.save(Wallet.builder().user(user).build());
                });
    }

    private void recordTx(Wallet wallet, WalletTransactionType type, BigDecimal amount,
                           BigDecimal before, BigDecimal after, String desc,
                           Long refId, String refType) {
        txRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(desc)
                .referenceId(refId)
                .referenceType(refType)
                .build());
    }

    private String resilienceLevel(Wallet w) {
        if (w.getEmergencyTargetAmount() == null || w.getEmergencyTargetAmount().compareTo(BigDecimal.ZERO) == 0)
            return "NON_CONFIGURE";
        double pct = w.getEmergencyFundBalance()
                .divide(w.getEmergencyTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        if (pct >= 100) return "EXCELLENT";
        if (pct >= 66)  return "PRET";
        if (pct >= 33)  return "EN_COURS";
        return "INSUFFISANT";
    }

    private WalletResponse toResponse(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .userId(w.getUser().getId())
                .availableBalance(w.getAvailableBalance())
                .emergencyFundBalance(w.getEmergencyFundBalance())
                .totalBalance(w.getTotalBalance())
                .status(w.getStatus())
                .emergencyTargetAmount(w.getEmergencyTargetAmount())
                .emergencyMonthlyContribution(w.getEmergencyMonthlyContribution())
                .emergencyAutoContribute(w.isEmergencyAutoContribute())
                .emergencyContributionDay(w.getEmergencyContributionDay())
                .emergencyProgressPct(w.getEmergencyProgressPct())
                .emergencyResilienceLevel(resilienceLevel(w))
                .createdAt(w.getCreatedAt())
                .build();
    }

    private WalletTransactionResponse toTxResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .referenceId(t.getReferenceId())
                .referenceType(t.getReferenceType())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private EmergencyWithdrawalResponse toWithdrawalResponse(EmergencyWithdrawal w) {
        User user = w.getWallet().getUser();
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName  = user.getLastName()  != null ? user.getLastName()  : "";
        String fullName  = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) fullName = user.getEmail();
        return EmergencyWithdrawalResponse.builder()
                .id(w.getId())
                .walletUserId(user.getId())
                .walletUserName(fullName)
                .walletUserEmail(user.getEmail())
                .amount(w.getAmount())
                .reason(w.getReason())
                .description(w.getDescription())
                .status(w.getStatus())
                .adminNote(w.getAdminNote())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
