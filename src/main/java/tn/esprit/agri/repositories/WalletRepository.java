package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.agri.entities.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(String userId);
    boolean existsByUserId(String userId);

    @Query("SELECT COALESCE(SUM(w.availableBalance + w.emergencyFundBalance), 0) FROM Wallet w")
    BigDecimal totalPlatformWalletBalance();

    @Query("SELECT COALESCE(SUM(w.emergencyFundBalance), 0) FROM Wallet w")
    BigDecimal totalEmergencyFunds();

    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.emergencyFundBalance = 0")
    long countWithNoEmergencyFund();
}
