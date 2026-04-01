package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.dto_savings_accountability.GoalPriorityRequest;
import tn.esprit.agri.dto_savings_accountability.SavingsGoalRequest;
import tn.esprit.agri.dto_savings_accountability.SavingsGoalResponse;
import tn.esprit.agri.entities.SavingsAccount;
import tn.esprit.agri.entities.SavingsGoal;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.exception.ResourceNotFoundException;
import tn.esprit.agri.repositories.SavingsAccountRepository;
import tn.esprit.agri.services.ISavingsGoalService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/savings/goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final ISavingsGoalService goalService;
    private final SavingsAccountRepository savingsAccountRepository;

    /** Créer un nouvel objectif d'épargne */
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsGoalResponse> createGoal(
            @AuthenticationPrincipal User user,
            @RequestBody SavingsGoalRequest request) {

        SavingsAccount account = getAccount(user);

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .customAllocationPercentage(request.getCustomAllocationPercentage())
                .build();

        SavingsGoal saved = goalService.createGoalForAccount(account.getId(), goal);
        return ResponseEntity.status(201).body(toResponse(saved));
    }

    /** Lister tous les objectifs (actifs) de l'utilisateur */
    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<SavingsGoalResponse>> getMyGoals(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean achieved) {

        SavingsAccount account = getAccount(user);
        List<SavingsGoal> goals = achieved != null
                ? goalService.getGoalsByAccountIdAndAchievement(account.getId(), achieved)
                : goalService.getGoalsByAccountId(account.getId());

        return ResponseEntity.ok(goals.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Détail d'un objectif */
    @GetMapping("/{goalId}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsGoalResponse> getGoal(@PathVariable String goalId) {
        return ResponseEntity.ok(toResponse(goalService.getGoalById(goalId)));
    }

    /** Modifier un objectif (nom, montant, date, description) */
    @PutMapping("/{goalId}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsGoalResponse> updateGoal(
            @PathVariable String goalId,
            @RequestBody SavingsGoalRequest request) {

        SavingsGoal goalDetails = SavingsGoal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .description(request.getDescription())
                .priority(request.getPriority())
                .customAllocationPercentage(request.getCustomAllocationPercentage())
                .build();

        return ResponseEntity.ok(toResponse(goalService.updateGoal(goalId, goalDetails)));
    }

    /**
     * Modifier la priorité et/ou l'allocation personnalisée d'un objectif.
     *
     * Exemples de body :
     * { "priority": 1 }                            → objectif financé en premier
     * { "customAllocationPercentage": 40.00 }      → 40% du solde alloué à cet objectif
     * { "priority": 2, "customAllocationPercentage": 30.00 }
     * { "resetToAuto": true }                      → revenir à l'allocation automatique
     */
    @PatchMapping("/{goalId}/priority")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsGoalResponse> updateGoalPriority(
            @PathVariable String goalId,
            @RequestBody GoalPriorityRequest request) {

        return ResponseEntity.ok(toResponse(goalService.updateGoalPriority(goalId, request)));
    }

    /** Collecter un objectif atteint (archivage + retrait du montant) */
    @PostMapping("/{goalId}/collect")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<SavingsGoalResponse> collectGoal(@PathVariable String goalId) {
        return ResponseEntity.ok(toResponse(goalService.collectGoal(goalId)));
    }

    /** Lister les objectifs archivés (collectés) */
    @GetMapping("/archive")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<SavingsGoalResponse>> getArchivedGoals(@AuthenticationPrincipal User user) {
        SavingsAccount account = getAccount(user);
        return ResponseEntity.ok(
                goalService.getArchivedGoals(account.getId())
                        .stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /** Supprimer un objectif */
    @DeleteMapping("/{goalId}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> deleteGoal(@PathVariable String goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private SavingsAccount getAccount(User user) {
        return savingsAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings Account", "userId", user.getId()));
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        double progress = 0;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);

        String allocationMode;
        if (goal.getCustomAllocationPercentage() != null && goal.getCustomAllocationPercentage().compareTo(BigDecimal.ZERO) > 0) {
            allocationMode = "CUSTOM_PERCENTAGE";
        } else if (goal.getPriority() != null && goal.getPriority() > 0) {
            allocationMode = "AUTO_PRIORITY";
        } else {
            allocationMode = "AUTO_PROPORTIONAL";
        }

        return SavingsGoalResponse.builder()
                .id(goal.getId())
                .savingsAccountId(goal.getSavingsAccount().getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remainingAmount(remaining)
                .targetDate(goal.getTargetDate())
                .description(goal.getDescription())
                .achieved(goal.getAchieved())
                .collected(goal.getCollected())
                .collectedAt(goal.getCollectedAt())
                .progressPercentage(Math.round(progress * 100.0) / 100.0)
                .priority(goal.getPriority())
                .customAllocationPercentage(goal.getCustomAllocationPercentage())
                .allocationMode(allocationMode)
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
