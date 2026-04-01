package tn.esprit.agri.services;

import tn.esprit.agri.dto_savings_accountability.GoalPriorityRequest;
import tn.esprit.agri.entities.SavingsGoal;

import java.math.BigDecimal;
import java.util.List;

public interface ISavingsGoalService {

    SavingsGoal createGoalForAccount(Long savingsAccountId, SavingsGoal goal);

    SavingsGoal getGoalById(String goalId);

    List<SavingsGoal> getGoalsByAccountId(Long savingsAccountId);

    List<SavingsGoal> getGoalsByAccountIdAndAchievement(Long savingsAccountId, Boolean achieved);

    SavingsGoal updateGoal(String goalId, SavingsGoal goalDetails);

    void deleteGoal(String goalId);

    SavingsGoal collectGoal(String goalId);

    List<SavingsGoal> getArchivedGoals(Long savingsAccountId);

    /**
     * Met à jour la priorité et/ou le pourcentage d'allocation personnalisé d'un objectif,
     * puis relance la synchronisation.
     *
     * Algorithme de distribution (syncGoalsWithBalance) :
     * 1. Les objectifs avec customAllocationPercentage reçoivent exactement ce % du solde.
     *    (plafonné à leur montant cible)
     * 2. Le solde restant (100% - somme des %) est distribué aux objectifs sans % fixe :
     *    a. Ceux avec priority > 0 : remplis séquentiellement (le plus haut d'abord).
     *    b. Ceux avec priority = 0 : distribution proportionnelle à leur montant cible.
     */
    SavingsGoal updateGoalPriority(String goalId, GoalPriorityRequest request);

    /**
     * Distribue automatiquement le solde du compte parmi les objectifs actifs (non archivés)
     * selon les règles de priorité et d'allocation personnalisée.
     * Met à jour account.goalAmount et account.goalTitle.
     */
    void syncGoalsWithBalance(Long accountId, BigDecimal currentBalance);
}
