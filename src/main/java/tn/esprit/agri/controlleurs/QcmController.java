package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.exception.BusinessRuleException;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.AiVerificationService;
import tn.esprit.agri.DTO.AiQcmResponse;
import java.util.UUID;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qcm")
@RequiredArgsConstructor
public class QcmController {

    private final QcmTestRepository qcmTestRepository;
    private final FarmerQcmResultRepository farmerQcmResultRepository;
    private final FarmerSolidarityFundRepository farmerSolidarityFundRepository;
    private final SolidarityFundRepository solidarityFundRepository;
    private final AiVerificationService aiVerificationService;

    // ✅ ADMIN crée un test
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QcmTest> createQcmTest(@RequestBody QcmTest test) {

        if (test.getQuestions() != null) {
            for (QcmQuestion question : test.getQuestions()) {
                question.setTest(test);
            }
        }

        return ResponseEntity.ok(qcmTestRepository.save(test));
    }

    // ✅ ADMIN génère un test par IA pour un fonds spécifique
    @PostMapping("/generate/{fundId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QcmTest> generateQcmForFund(@PathVariable String fundId) {
        SolidarityFund fund = solidarityFundRepository.findById(fundId)
                .orElseThrow(() -> new BusinessRuleException("Fonds de solidarité non trouvé", "FUND_NOT_FOUND"));

        List<tn.esprit.agri.entities.QuestionBank> bankQuestions = aiVerificationService.getRandomQuestionsFromBank(fund.getCultureType(), 5);

        QcmTest test = fund.getTest();
        if (test != null) {
            // Update existing test
            test.getQuestions().clear();
            test.setTitle("Test d'expertise (Mise à jour) : " + fund.getCultureType());
            test.setEndDate(LocalDateTime.now().plusMonths(1));
        } else {
            // Create new test
            test = QcmTest.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Test d'expertise : " + fund.getCultureType())
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(1))
                    .requiredScore(60.0) // Require 60% passed
                    .discountPercentage(20.0)
                    .fund(fund)
                    .questions(new ArrayList<>())
                    .build();
        }

        for (tn.esprit.agri.entities.QuestionBank bankQ : bankQuestions) {
            QcmQuestion question = QcmQuestion.builder()
                    .id(UUID.randomUUID().toString())
                    .test(test)
                    .text(bankQ.getText())
                    .options(new ArrayList<>(bankQ.getOptions()))
                    .correctAnswer(bankQ.getCorrectAnswer())
                    .build();
            test.getQuestions().add(question);
        }

        return ResponseEntity.ok(qcmTestRepository.save(test));
    }

    // ✅ FARMER voit les tests actifs (ADMIN peut aussi les lister)
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailableTests(Authentication authentication) {
        LocalDateTime now = LocalDateTime.now();
        List<QcmTest> tests = qcmTestRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(now, now);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<String> farmerFundIds = new ArrayList<>();
        if (!isAdmin) {
            User farmer = (User) authentication.getPrincipal();
            farmerSolidarityFundRepository.findByFarmerId(farmer.getId())
                    .forEach(m -> farmerFundIds.add(m.getSolidarityFund().getId()));
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (QcmTest test : tests) {
            // Filter: Farmer only sees tests for funds they joined
            if (!isAdmin && test.getFund() != null && !farmerFundIds.contains(test.getFund().getId())) {
                continue;
            }

            Map<String, Object> testMap = new java.util.HashMap<>();
            testMap.put("id", test.getId());
            testMap.put("title", test.getTitle());
            testMap.put("startDate", test.getStartDate());
            testMap.put("endDate", test.getEndDate());
            testMap.put("requiredScore", test.getRequiredScore());
            testMap.put("discountPercentage", test.getDiscountPercentage());
            if (test.getFund() != null) {
                testMap.put("fund", Map.of("id", test.getFund().getId(), "name", test.getFund().getName()));
            }

            List<Map<String, Object>> questionsList = new ArrayList<>();
            for (QcmQuestion q : test.getQuestions()) {
                Map<String, Object> qMap = new java.util.HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("text", q.getText());
                qMap.put("options", q.getOptions());
                if (isAdmin) {
                    qMap.put("correctAnswer", q.getCorrectAnswer());
                }
                questionsList.add(qMap);
            }
            testMap.put("questions", questionsList);
            response.add(testMap);
        }

        return ResponseEntity.ok(response);
    }

    // ✅ FARMER récupère les questions (sans réponses correctes)
    @GetMapping("/{testId}/questions")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<Map<String, Object>>> getQuestions(
            @PathVariable String testId,
            @RequestParam(defaultValue = "5") int limit) {

        QcmTest test = qcmTestRepository.findById(testId)
                .orElseThrow(() -> new BusinessRuleException("Test non trouvé", "TEST_NOT_FOUND"));

        List<QcmQuestion> questions = test.getQuestions();
        if (questions.size() > limit) {
            questions = questions.subList(0, limit);
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (QcmQuestion q : questions) {
            Map<String, Object> qMap = new java.util.HashMap<>();
            qMap.put("id", q.getId());
            qMap.put("text", q.getText());
            qMap.put("options", q.getOptions());
            // FARMER never gets the correct answer here
            response.add(qMap);
        }

        return ResponseEntity.ok(response);
    }

    // ✅ FARMER soumet ses réponses
    @PostMapping("/{testId}/submit")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<String> submitTest(
            @PathVariable String testId,
            @RequestBody Map<String, String> answers,
            Authentication authentication) {

        User farmer = (User) authentication.getPrincipal();

        QcmTest test = qcmTestRepository.findById(testId)
                .orElseThrow(() -> new BusinessRuleException("Test non trouvé", "TEST_NOT_FOUND"));

        // ✅ Vérifier si déjà réussi ce mois-ci
        FarmerQcmResult lastResult = farmerQcmResultRepository
                .findTopByFarmerIdAndTestIdOrderByCompletedAtDesc(farmer.getId(), testId);

        if (lastResult != null &&
                lastResult.isPassed() &&
                lastResult.getDiscountValidUntil() != null &&
                !lastResult.getDiscountValidUntil().isBefore(LocalDate.now())) {

            throw new BusinessRuleException(
                    "Vous avez déjà bénéficié de la réduction ce mois-ci.",
                    "ALREADY_BENEFITED");
        }

        int score = 0;
        int total = test.getQuestions().size();

        for (QcmQuestion q : test.getQuestions()) {

            String submitted = answers.get(q.getId());

            if (submitted != null &&
                    q.getCorrectAnswer().trim().equalsIgnoreCase(submitted.trim())) {
                score++;
            }
        }

        double percentage = (double) score / total * 100;
        boolean passed = percentage >= test.getRequiredScore();

        LocalDate today = LocalDate.now();
        LocalDate expiry = today.withDayOfMonth(today.lengthOfMonth());

        FarmerQcmResult result = FarmerQcmResult.builder()
                .farmer(farmer)
                .test(test)
                .completedAt(LocalDateTime.now())
                .score(score)
                .passed(passed)
                .discountValidUntil(passed ? expiry : null)
                .build();

        farmerQcmResultRepository.save(result);

        if (passed) {

            // double reducedRate = 1 - (20.0 / 100); // Fixed 20%

            // Apply to the specific fund associated with this test
            if (test.getFund() != null) {
                tn.esprit.agri.entities.FarmerSolidarityFund membership = farmerSolidarityFundRepository
                        .findByFarmerIdAndSolidarityFundId(
                                farmer.getId(),
                                test.getFund().getId())
                        .orElse(null);

                if (membership != null) {
                    membership.setDiscountPercentage(20.0);
                    farmerSolidarityFundRepository.save(membership);
                }
            }

            return ResponseEntity.ok(
                    "Test réussi ! Score: " + score + "/" + total +
                            ". Réduction de 20% appliquée sur le fonds.");
        }

        return ResponseEntity.ok(
                "Test terminé. Score: " + score + "/" + total +
                        ". Minimum requis: " + test.getRequiredScore() +
                        "%. Pas de réduction.");
    }

    @GetMapping("/my-results")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<FarmerQcmResult>> getMyResults(Authentication authentication) {
        User farmer = (User) authentication.getPrincipal();
        return ResponseEntity.ok(farmerQcmResultRepository.findByFarmerId(farmer.getId()));
    }
}
