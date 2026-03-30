package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.SavingsGoal;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, String> {
    List<SavingsGoal> findBySavingsAccountIdOrderByTargetDateAsc(Long savingsAccountId);
    List<SavingsGoal> findBySavingsAccountIdAndAchieved(Long savingsAccountId, Boolean achieved);

    List<SavingsGoal> findBySavingsAccountIdAndAchievedFalseOrderByTargetDateAsc(Long savingsAccountId);

    @Query("SELECT COALESCE(SUM(g.targetAmount), 0) FROM SavingsGoal g WHERE g.savingsAccount.id = :accountId AND g.achieved = false")
    BigDecimal sumTargetAmountByAccountIdAndNotAchieved(@Param("accountId") Long accountId);

    List<SavingsGoal> findBySavingsAccountIdAndCollectedTrue(Long savingsAccountId);
}
