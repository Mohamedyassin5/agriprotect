package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.SavingsTransaction;
import tn.esprit.agri.entities.enums.SavingsTransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findByAccountIdOrderByOccurredAtDesc(Long accountId);

    List<SavingsTransaction> findByAccountIdAndOccurredAtBetween(Long accountId, LocalDateTime start, LocalDateTime end);

    List<SavingsTransaction> findByAccountIdAndType(Long accountId, SavingsTransactionType type);

    Page<SavingsTransaction> findByAccountId(Long accountId, Pageable pageable);

    Optional<SavingsTransaction> findByIdAndAccountId(Long id, Long accountId);

    @Query("SELECT st FROM SavingsTransaction st WHERE st.account.id = :accountId " +
            "AND (:type IS NULL OR st.type = :type) " +
            "AND (:startDate IS NULL OR st.occurredAt >= :startDate) " +
            "AND (:endDate IS NULL OR st.occurredAt <= :endDate)")
    Page<SavingsTransaction> findByFilters(@Param("accountId") Long accountId,
                                           @Param("type") SavingsTransactionType type,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    // --- AI Feature queries ---
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SavingsTransaction st WHERE st.account.id = :accountId AND st.type = :type AND st.occurredAt BETWEEN :start AND :end")
    java.math.BigDecimal sumAmountByAccountIdAndTypeAndDateRange(@Param("accountId") Long accountId,
                                                                 @Param("type") SavingsTransactionType type,
                                                                 @Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(st) FROM SavingsTransaction st WHERE st.account.id = :accountId AND st.type = :type AND st.occurredAt BETWEEN :start AND :end")
    Long countByAccountIdAndTypeAndDateRange(@Param("accountId") Long accountId,
                                             @Param("type") SavingsTransactionType type,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
