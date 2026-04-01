package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.dto_savings_accountability.GoalPriorityRequest;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsGoal;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.repositories.SavingsGoalRepository;
import tn.esprit.agri.services.ISavingsGoalService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsGoalServiceImpl implements ISavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    public SavingsGoal createGoalForAccount(Long savingsAccountId, SavingsGoal goal) {
        SavingsAccount account = savingsAccountRepository.findById(savingsAccountId)
                .orElseThrow(() -> new RuntimeException("Savings account not found: " + savingsAccountId));

        goal.setSavingsAccount(account);
        goalRepository.save(goal);
        syncGoalsWithBalance(savingsAccountId, account.getCurrentBalance());
        return goalRepository.findById(goal.getId()).orElse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsGoal getGoalById(String goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoal> getGoalsByAccountId(Long savingsAccountId) {
        return goalRepository.findBySavingsAccountIdOrderByTargetDateAsc(savingsAccountId)
                .stream().filter(g -> !g.getCollected()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoal> getGoalsByAccountIdAndAchievement(Long savingsAccountId, Boolean achieved) {
        return goalRepository.findBySavingsAccountIdAndAchieved(savingsAccountId, achieved);
    }

    @Override
    public SavingsGoal updateGoal(String goalId, SavingsGoal goalDetails) {
        SavingsGoal existing = getGoalById(goalId);

        if (goalDetails.getGoalName() != null) existing.setGoalName(goalDetails.getGoalName());
        if (goalDetails.getTargetAmount() != null) existing.setTargetAmount(goalDetails.getTargetAmount());
        if (goalDetails.getTargetDate() != null) existing.setTargetDate(goalDetails.getTargetDate());
        if (goalDetails.getDescription() != null) existing.setDescription(goalDetails.getDescription());
        if (goalDetails.getPriority() != null) existing.setPriority(goalDetails.getPriority());
        if (goalDetails.getCustomAllocationPercentage() != null) {
            validateCustomAllocationPercentage(existing.getSavingsAccount().getId(), goalId,
                    goalDetails.getCustomAllocationPercentage());
            existing.setCustomAllocationPercentage(goalDetails.getCustomAllocationPercentage());
        }

        goalRepository.save(existing);
        syncGoalsWithBalance(existing.getSavingsAccount().getId(), existing.getSavingsAccount().getCurrentBalance());
        return goalRepository.findById(existing.getId()).orElse(existing);
    }

    @Override
    public void deleteGoal(String goalId) {
        SavingsGoal goal = getGoalById(goalId);
        Long accountId = goal.getSavingsAccount().getId();
        BigDecimal balance = goal.getSavingsAccount().getCurrentBalance();
        goalRepository.delete(goal);
        goalRepository.flush();
        syncGoalsWithBalance(accountId, balance);
    }

    @Override
    public SavingsGoal collectGoal(String goalId) {
        SavingsGoal goal = getGoalById(goalId);
        if (!goal.getAchieved()) throw new IllegalArgumentException(
                "Impossible de collecter un objectif non atteint. Supprimez-le pour redistribuer les fonds.");
        if (goal.getCollected()) throw new IllegalArgumentException("Cet objectif est déjà collecté.");

        SavingsAccount account = goal.getSavingsAccount();
        BigDecimal newBalance = account.getCurrentBalance().subtract(goal.getTargetAmount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Solde insuffisant pour collecter cet objectif.");

        account.setCurrentBalance(newBalance);
        savingsAccountRepository.save(account);

        goal.setCollected(true);
        goal.setCollectedAt(LocalDateTime.now());
        goal.setCurrentAmount(goal.getTargetAmount());
        goalRepository.save(goal);

        syncGoalsWithBalance(account.getId(), newBalance);
        return goalRepository.findById(goal.getId()).orElse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoal> getArchivedGoals(Long savingsAccountId) {
        return goalRepository.findBySavingsAccountIdAndCollectedTrue(savingsAccountId);
    }

    // ============================================================
    // Mise à jour de la priorité / allocation personnalisée
    // ============================================================

    @Override
    public SavingsGoal updateGoalPriority(String goalId, GoalPriorityRequest request) {
        SavingsGoal goal = getGoalById(goalId);

        if (Boolean.TRUE.equals(request.getResetToAuto())) {
            // Réinitialisation complète : tout en automatique proportionnel
            goal.setCustomAllocationPercentage(null);
            goal.setPriority(0);
        } else if (request.getCustomAllocationPercentage() != null) {
            // Mode CUSTOM_PERCENTAGE : efface la priorité (mutuellement exclusif)
            if (request.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) < 0
                    || request.getCustomAllocationPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Le pourcentage doit être entre 0 et 100.");
            }
            validateCustomAllocationPercentage(goal.getSavingsAccount().getId(), goalId,
                    request.getCustomAllocationPercentage());
            goal.setCustomAllocationPercentage(request.getCustomAllocationPercentage());
            goal.setPriority(0); // priorité incompatible avec % fixe
        } else if (request.getPriority() != null) {
            // Mode PRIORITY ou AUTO : efface le % fixe (mutuellement exclusif)
            if (request.getPriority() < 0)
                throw new IllegalArgumentException("La priorité doit être >= 0 (0 = automatique).");
            goal.setPriority(request.getPriority());
            goal.setCustomAllocationPercentage(null); // % fixe incompatible avec priorité
        }

        goalRepository.save(goal);
        SavingsAccount account = goal.getSavingsAccount();
        syncGoalsWithBalance(account.getId(), account.getCurrentBalance());
        return goalRepository.findById(goal.getId()).orElse(goal);
    }

    /**
     * Vérifie que la somme de tous les customAllocationPercentage (y compris la nouvelle valeur)
     * ne dépasse pas 100%.
     */
    private void validateCustomAllocationPercentage(Long accountId, String excludeGoalId, BigDecimal newPct) {
        List<SavingsGoal> otherGoals = goalRepository
                .findBySavingsAccountIdOrderByTargetDateAsc(accountId)
                .stream()
                .filter(g -> !g.getCollected() && !g.getId().equals(excludeGoalId))
                .toList();

        BigDecimal sumOthers = otherGoals.stream()
                .filter(g -> g.getCustomAllocationPercentage() != null)
                .map(SavingsGoal::getCustomAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumOthers.add(newPct).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "La somme des pourcentages personnalisés dépasse 100%. "
                    + "Les autres objectifs utilisent déjà " + sumOthers + "%. "
                    + "Maximum disponible pour cet objectif : "
                    + BigDecimal.valueOf(100).subtract(sumOthers) + "%.");
        }
    }

    // ============================================================
    // Synchronisation intelligente du solde entre les objectifs
    // ============================================================

    /**
     * Algorithme de distribution du solde entre les objectifs actifs :
     *
     * ÉTAPE 1 — Objectifs avec pourcentage fixe (customAllocationPercentage != null)
     *   → Reçoivent exactement (solde × %) du compte, plafonné à leur montant cible.
     *
     * ÉTAPE 2 — Solde résiduel (100% - somme des % fixes) distribué aux autres objectifs :
     *   A. Objectifs avec priority > 0 : remplis séquentiellement, du plus prioritaire au moins.
     *      Ex : priorité 1 reçoit tout ce qu'il faut jusqu'à son montant cible,
     *           puis priorité 2 reçoit le reste, etc.
     *   B. Objectifs avec priority = 0 : distribution proportionnelle à leur montant cible.
     */
    @Override
    public void syncGoalsWithBalance(Long accountId, BigDecimal currentBalance) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Savings account not found: " + accountId));

        List<SavingsGoal> allActive = goalRepository
                .findBySavingsAccountIdOrderByTargetDateAsc(accountId)
                .stream().filter(g -> !g.getCollected()).toList();

        if (allActive.isEmpty()) {
            updateAccountGoalFields(account, allActive);
            return;
        }

        // Séparer : objectifs à % fixe vs objectifs automatiques
        List<SavingsGoal> fixedPctGoals = allActive.stream()
                .filter(g -> g.getCustomAllocationPercentage() != null
                        && g.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<SavingsGoal> autoGoals = allActive.stream()
                .filter(g -> g.getCustomAllocationPercentage() == null
                        || g.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) == 0)
                .toList();

        // --- ÉTAPE 1 : allouer les objectifs à % fixe ---
        BigDecimal totalFixedPct = fixedPctGoals.stream()
                .map(SavingsGoal::getCustomAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (SavingsGoal g : fixedPctGoals) {
            BigDecimal allocated = currentBalance
                    .multiply(g.getCustomAllocationPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .min(g.getTargetAmount());
            g.setCurrentAmount(allocated.max(BigDecimal.ZERO));
            g.setAchieved(g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0);
            goalRepository.save(g);
        }

        // --- ÉTAPE 2 : solde résiduel pour les objectifs automatiques ---
        BigDecimal residualPct = BigDecimal.valueOf(100).subtract(totalFixedPct);
        BigDecimal residualBalance = currentBalance
                .multiply(residualPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);

        if (!autoGoals.isEmpty()) {
            // Séparer : priorité > 0 (séquentiel) vs priorité = 0 (proportionnel)
            List<SavingsGoal> priorityGoals = autoGoals.stream()
                    .filter(g -> g.getPriority() > 0)
                    .sorted(Comparator.comparingInt(SavingsGoal::getPriority))
                    .toList();

            List<SavingsGoal> proportionalGoals = autoGoals.stream()
                    .filter(g -> g.getPriority() == 0)
                    .toList();

            // 2A — Remplissage séquentiel par priorité
            BigDecimal remainingAfterPriority = residualBalance;
            for (SavingsGoal g : priorityGoals) {
                BigDecimal allocated = remainingAfterPriority.min(g.getTargetAmount());
                g.setCurrentAmount(allocated.max(BigDecimal.ZERO));
                g.setAchieved(g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0);
                remainingAfterPriority = remainingAfterPriority.subtract(allocated).max(BigDecimal.ZERO);
                goalRepository.save(g);
            }

            // 2B — Distribution proportionnelle pour les objectifs sans priorité
            BigDecimal totalProportionalTarget = proportionalGoals.stream()
                    .map(SavingsGoal::getTargetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (SavingsGoal g : proportionalGoals) {
                BigDecimal allocated;
                if (totalProportionalTarget.compareTo(BigDecimal.ZERO) > 0) {
                    allocated = remainingAfterPriority
                            .multiply(g.getTargetAmount())
                            .divide(totalProportionalTarget, 2, RoundingMode.HALF_UP)
                            .min(g.getTargetAmount());
                } else {
                    allocated = BigDecimal.ZERO;
                }
                g.setCurrentAmount(allocated.max(BigDecimal.ZERO));
                g.setAchieved(g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0);
                goalRepository.save(g);
            }
        }

        updateAccountGoalFields(account, allActive);
    }

    private void updateAccountGoalFields(SavingsAccount account, List<SavingsGoal> allGoals) {
        List<SavingsGoal> activeGoals = allGoals.stream().filter(g -> !g.getAchieved()).toList();
        long achievedCount = allGoals.stream().filter(SavingsGoal::getAchieved).count();

        if (allGoals.isEmpty()) {
            account.setGoalAmount(null);
            account.setGoalTitle(null);
        } else if (activeGoals.isEmpty()) {
            account.setGoalAmount(BigDecimal.ZERO);
            account.setGoalTitle(achievedCount + " objectif(s) atteint(s) ✓");
        } else {
            BigDecimal totalTarget = activeGoals.stream()
                    .map(SavingsGoal::getTargetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            account.setGoalAmount(totalTarget);

            // Afficher par priorité : priorité 1 en premier, puis 2, puis les auto
            String goalNames = allGoals.stream()
                    .filter(g -> !g.getAchieved())
                    .sorted(Comparator.comparingInt((SavingsGoal g) -> g.getPriority() == 0 ? Integer.MAX_VALUE : g.getPriority()))
                    .map(g -> {
                        String label = g.getGoalName();
                        if (g.getPriority() > 0) label += " [P" + g.getPriority() + "]";
                        if (g.getCustomAllocationPercentage() != null) label += " [" + g.getCustomAllocationPercentage() + "%]";
                        return label;
                    })
                    .collect(Collectors.joining(", "));

            account.setGoalTitle(activeGoals.size() + " objectif(s): " + goalNames
                    + (achievedCount > 0 ? " | " + achievedCount + " atteint(s)" : ""));
        }

        savingsAccountRepository.save(account);
    }
}
