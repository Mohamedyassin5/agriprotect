package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.dto_savings_accountability.TransactionRequest;
import tn.esprit.agri.dto_savings_accountability.TransactionResponse;
import tn.esprit.agri.entities.enums.SavingsTransactionType;

import java.time.LocalDateTime;

public interface ISavingsTransactionService {
    TransactionResponse createTransaction(String userId, TransactionRequest request);
    Page<TransactionResponse> getTransactions(String userId, SavingsTransactionType type,
                                              LocalDateTime from, LocalDateTime to, Pageable pageable);
    TransactionResponse getTransaction(Long id, String userId);
    void deleteTransaction(Long id, String userId);
}
