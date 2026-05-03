package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.WalletTransaction;
import tn.esprit.agri.entities.enums.WalletTransactionType;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<WalletTransaction> findByWalletIdAndType(Long walletId, WalletTransactionType type);
}
