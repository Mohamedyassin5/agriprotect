package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.Budget;
import tn.esprit.agri.entities.enums.BudgetPeriodType;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdOrderByPeriodStartDesc(String userId);

    List<Budget> findByUserIdAndPeriodType(String userId, BudgetPeriodType periodType);

    List<Budget> findByUserIdAndCategory(String userId, EntryCategory category);

    List<Budget> findByUserIdAndPeriodStartBetween(String userId, LocalDate startDate, LocalDate endDate);

    Page<Budget> findByUserId(String userId, Pageable pageable);

    Optional<Budget> findByIdAndUserId(Long id, String userId);

    boolean existsByUserIdAndCategoryAndPeriodStartAndPeriodEnd(
            String userId, EntryCategory category, LocalDate periodStart, LocalDate periodEnd);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND b.periodStart <= :date AND b.periodEnd >= :date " +
           "AND (:category IS NULL OR b.category = :category)")
    List<Budget> findActiveBudgets(@Param("userId") String userId,
                                  @Param("date") LocalDate date,
                                  @Param("category") EntryCategory category);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND (:periodType IS NULL OR b.periodType = :periodType) " +
           "AND (:category IS NULL OR b.category = :category) " +
           "AND (:startDate IS NULL OR b.periodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.periodEnd <= :endDate)")
    Page<Budget> findByFilters(@Param("userId") String userId,
                              @Param("periodType") BudgetPeriodType periodType,
                              @Param("category") EntryCategory category,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              Pageable pageable);
}
