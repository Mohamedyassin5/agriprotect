package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.enums.SavingsAccountStatus;

import java.util.List;
import java.util.Optional;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {
    Optional<SavingsAccount> findByUserId(String userId);
    List<SavingsAccount> findByStatus(SavingsAccountStatus status);
}
