package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.TransactionRequest;
import tn.esprit.agri.dto_savings_accountability.TransactionResponse;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsTransaction;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;
import tn.esprit.agri.entities.enums.SavingsTransactionType;
import tn.esprit.agri.exception.InsufficientFundsException;
import tn.esprit.agri.exception.ResourceNotFoundException;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.SavingsTransactionRepository;
import tn.esprit.agri.services.ISavingsGoalService;
import tn.esprit.agri.services.ISavingsTransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsTransactionServiceImpl implements ISavingsTransactionService {

    private final SavingsTransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final ISavingsGoalService savingsGoalService;

    @Override
    public TransactionResponse createTransaction(String userId, TransactionRequest request) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        if (account.getStatus() == SavingsAccountStatus.FROZEN) {
            throw new IllegalArgumentException("Account is frozen. Unfreeze it before making transactions.");
        }

        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal newBalance;

        if (request.getType() == SavingsTransactionType.DEPOSIT) {
            newBalance = currentBalance.add(request.getAmount());
        } else { // WITHDRAWAL
            if (currentBalance.compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException(currentBalance, request.getAmount());
            }
            newBalance = currentBalance.subtract(request.getAmount());
        }

        account.setCurrentBalance(newBalance);
        savingsAccountRepository.save(account);

        SavingsTransaction transaction = SavingsTransaction.builder()
                .account(account)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // Auto-sync goals with new balance
        savingsGoalService.syncGoalsWithBalance(account.getId(), newBalance);

        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(String userId, SavingsTransactionType type,
                                                     LocalDateTime from, LocalDateTime to, Pageable pageable) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        Page<SavingsTransaction> transactions = transactionRepository.findByFilters(
                account.getId(), type, from, to, pageable);

        return transactions.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id, String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        SavingsTransaction transaction = transactionRepository.findByIdAndAccountId(id, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        return mapToResponse(transaction);
    }

    @Override
    public void deleteTransaction(Long id, String userId) {
        SavingsAccount account = savingsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", userId));

        SavingsTransaction transaction = transactionRepository.findByIdAndAccountId(id, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // Reverse the transaction effect on balance
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal newBalance;

        if (transaction.getType() == SavingsTransactionType.DEPOSIT) {
            newBalance = currentBalance.subtract(transaction.getAmount());
        } else {
            newBalance = currentBalance.add(transaction.getAmount());
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot delete transaction: would result in negative balance");
        }

        account.setCurrentBalance(newBalance);
        savingsAccountRepository.save(account);

        transactionRepository.delete(transaction);

        // Auto-sync goals with corrected balance
        savingsGoalService.syncGoalsWithBalance(account.getId(), newBalance);
    }

    private TransactionResponse mapToResponse(SavingsTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .occurredAt(transaction.getOccurredAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
