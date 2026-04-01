package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.InsuranceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsuranceRepository extends JpaRepository<Insurance, String> {
    List<Insurance> findByUserAndStatus(User user, InsuranceStatus status);
    Optional<Insurance> findByIdAndUserId(String id, String userId);
    List<Insurance> findByStatus(InsuranceStatus status);
    // Compter par statut (utilisé dans les stats)
    long countByStatus(InsuranceStatus status);
    List<Insurance> findByStatusIn(List<InsuranceStatus> statuses);
    // Compter toutes les polices actives, overdue, etc.
    long countByStatusIn(List<InsuranceStatus> statuses);

    // Trouver les polices en retard ou suspendues
    List<Insurance> findByStatusInOrderByNextPaymentDueAsc(List<InsuranceStatus> statuses);

    // Optionnel : polices par utilisateur (pour le farmer)
    List<Insurance> findByUserIdOrderByCreatedAtDesc(String userId);

    // Optionnel : compter les polices d'un agriculteur
    long countByUserId(String userId);


    long countByUserIdAndStatus(String userId, InsuranceStatus status);

    @Query("""
    SELECT i.nextPaymentDue 
    FROM Insurance i 
    WHERE i.user.id = :userId 
      AND i.status IN ('ACTIVE', 'OVERDUE', 'SUSPENDED') 
      AND i.nextPaymentDue IS NOT NULL
    ORDER BY i.nextPaymentDue ASC
    LIMIT 1
""")
    Optional<LocalDate> findNextPaymentDueByUserId(@Param("userId") String userId);

    @Query("""
    SELECT COALESCE(SUM(i.premiumAmount), 0) 
    FROM Insurance i 
    WHERE i.user.id = :userId 
      AND i.status IN ('ACTIVE', 'OVERDUE', 'SUSPENDED') 
      AND YEAR(i.startDate) = YEAR(CURRENT_DATE)
""")
    BigDecimal calculateTotalPremiumDueThisYear(@Param("userId") String userId);
}