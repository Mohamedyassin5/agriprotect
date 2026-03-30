package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.AccountingEntry;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {
    List<AccountingEntry> findByUserIdOrderByEntryDateDesc(String userId);

    List<AccountingEntry> findByUserIdAndEntryType(String userId, EntryType type);

    List<AccountingEntry> findByUserIdAndCategory(String userId, EntryCategory category);

    List<AccountingEntry> findByUserIdAndEntryDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    Page<AccountingEntry> findByUserId(String userId, Pageable pageable);

    Optional<AccountingEntry> findByIdAndUserId(Long id, String userId);

    @Query("SELECT ae FROM AccountingEntry ae WHERE ae.user.id = :userId " +
            "AND (:type IS NULL OR ae.entryType = :type) " +
            "AND (:category IS NULL OR ae.category = :category) " +
            "AND (:startDate IS NULL OR ae.entryDate >= :startDate) " +
            "AND (:endDate IS NULL OR ae.entryDate <= :endDate)")
    Page<AccountingEntry> findByFilters(@Param("userId") String userId,
                                        @Param("type") EntryType type,
                                        @Param("category") EntryCategory category,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") String userId, @Param("type") EntryType type);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type AND e.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(@Param("userId") String userId,
                                                    @Param("type") EntryType type,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type AND e.category = :category AND e.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCategoryAndDateRange(@Param("userId") String userId,
                                               @Param("type") EntryType type,
                                               @Param("category") EntryCategory category,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(e) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type AND e.entryDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndTypeAndDateRange(@Param("userId") String userId,
                                          @Param("type") EntryType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(AVG(e.amount), 0) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type AND e.category = :category AND e.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal averageAmountByCategoryAndDateRange(@Param("userId") String userId,
                                                   @Param("type") EntryType type,
                                                   @Param("category") EntryCategory category,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    // --- AI Feature queries ---
    @Query("SELECT COUNT(DISTINCT e.category) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type")
    Long countDistinctCategoriesByUserIdAndType(@Param("userId") String userId, @Param("type") EntryType type);

    @Query(value = "SELECT COUNT(DISTINCT MONTH(e.entry_date)) FROM accounting_entry e WHERE e.user_id = :userId AND e.entry_type = :type AND e.entry_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    Long countDistinctMonthsWithEntries(@Param("userId") String userId, @Param("type") String type,
                                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM AccountingEntry e WHERE e.user.id = :userId AND e.entryType = :type AND e.category = :category")
    BigDecimal sumAmountByUserIdAndTypeAndCategory(@Param("userId") String userId, @Param("type") EntryType type, @Param("category") EntryCategory category);

    List<AccountingEntry> findByUserIdAndEntryTypeAndCategoryAndEntryDateBetween(
            String userId, EntryType entryType, EntryCategory category, LocalDate startDate, LocalDate endDate);
}