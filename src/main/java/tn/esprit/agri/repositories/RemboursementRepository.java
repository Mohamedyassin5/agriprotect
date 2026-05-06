package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.Remboursement;
import tn.esprit.agri.entities.enums.StatutRemboursement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RemboursementRepository extends JpaRepository<Remboursement, String> {

    boolean existsBySinistreId(String sinistreId);

    List<Remboursement> findByInsuranceUserId(String userId);

    List<Remboursement> findByStatut(StatutRemboursement statut);

    Page<Remboursement> findByStatut(StatutRemboursement statut, Pageable pageable);

    Optional<Remboursement> findByStripeRefundId(String stripeRefundId);

    long countByStatut(StatutRemboursement statut);
    boolean existsBySinistreIdAndStatutNot(String sinistreId, StatutRemboursement statut);

    long countByInsuranceUserIdAndStatutAndDateRemboursementAfter(
            String userId, StatutRemboursement statut, LocalDate date);

    @Query("SELECT COALESCE(SUM(r.montantFinalRembourse), 0) " +
            "FROM Remboursement r WHERE r.statut = :statut")
    BigDecimal sumMontantFinalRembourseByStatut(@Param("statut") StatutRemboursement statut);

    // ── Anti-fraude : farmers avec trop de remboursements sur une période ────────
    // Retourne les remboursements PAYE appartenant à des farmers qui ont atteint
    // ou dépassé le seuil "max" sur la période débutant à "dateDebut".
    @Query("SELECT r FROM Remboursement r " +
            "WHERE r.statut = tn.esprit.agri.entities.enums.StatutRemboursement.PAYE " +
            "AND r.dateRemboursement >= :dateDebut " +
            "AND r.insurance.user.id IN (" +
            "   SELECT r2.insurance.user.id FROM Remboursement r2 " +
            "   WHERE r2.statut = tn.esprit.agri.entities.enums.StatutRemboursement.PAYE " +
            "   AND r2.dateRemboursement >= :dateDebut " +
            "   GROUP BY r2.insurance.user.id " +
            "   HAVING COUNT(r2) >= :max" +
            ")")
    List<Remboursement> findSuspects(@Param("dateDebut") LocalDate dateDebut, @Param("max") int max);
    Optional<Remboursement> findFirstByInsuranceUserIdAndStatutOrderByDateRemboursementDesc(
            String userId, StatutRemboursement statut);
}