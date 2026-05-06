package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.EmergencyWithdrawal;
import tn.esprit.agri.entities.enums.EmergencyWithdrawalStatus;

import java.util.List;

public interface EmergencyWithdrawalRepository extends JpaRepository<EmergencyWithdrawal, Long> {
    List<EmergencyWithdrawal> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<EmergencyWithdrawal> findByStatus(EmergencyWithdrawalStatus status);
    long countByWalletIdAndStatus(Long walletId, EmergencyWithdrawalStatus status);
}
