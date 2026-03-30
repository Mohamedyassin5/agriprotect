package tn.esprit.agri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final QcmTestRepository testRepository;
    private final SolidarityFundRepository fundRepository;
    private final FarmerQcmResultRepository resultRepository;
    private final FarmerSolidarityFundRepository farmerSolidarityFundRepository;
    private final UserRepository userRepository;

    public QcmTest createTestForFund(String fundId, String title, double requiredScore, List<QcmQuestion> questions) {
        SolidarityFund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new RuntimeException("Fund not found"));

        if (fund.getTest() != null) {
            throw new RuntimeException("This fund already has a test.");
        }

        // Test ID = Title (as per requirement)
        QcmTest test = QcmTest.builder()
                .id(title)
                .title(title)
                .requiredScore(requiredScore)
                .startDate(LocalDateTime.now())
                .fund(fund)
                .build();

        // Link questions and generate IDs
        int qIndex = 1;
        for (QcmQuestion q : questions) {
            q.setId(title + " - q" + qIndex++);
            q.setTest(test);
        }
        test.setQuestions(questions);

        return testRepository.save(test);
    }

    public QcmTest getTestByFund(String fundId) {
        SolidarityFund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new RuntimeException("Fund not found"));
        return fund.getTest();
    }

    public FarmerQcmResult submitTest(String userId, String testId, java.util.Map<String, String> answers) {
        User farmer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QcmTest test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Check if already passed
        boolean alreadyPassed = resultRepository.existsByFarmerIdAndTestIdAndPassedTrue(userId, testId);
        if (alreadyPassed) {
            throw new RuntimeException("You have already passed this test. Retaking is not allowed.");
        }

        // Calculate Score
        int correctCount = 0;
        List<QcmQuestion> questions = test.getQuestions();

        for (QcmQuestion q : questions) {
            String submittedAnswer = answers.get(q.getId());
            if (submittedAnswer != null && q.getCorrectAnswer().equalsIgnoreCase(submittedAnswer)) {
                correctCount++;
            }
        }

        // Logic check: score calculation (assuming 1 point per question for simplicity
        // or percentage)
        // Adjusting to percentage for generic check or raw score matching requiredScore
        // Let's assume requiredScore is total number of correct answers for now or
        // percentage.
        // Given complexity, let's use percentage: (correct / total) * 100
        double scorePercentage = ((double) correctCount / questions.size()) * 100;
        boolean passed = scorePercentage >= test.getRequiredScore();

        FarmerQcmResult result = FarmerQcmResult.builder()
                .farmer(farmer)
                .test(test)
                .completedAt(LocalDateTime.now())
                .score((int) scorePercentage) // Storing percentage as int score
                .passed(passed)
                .build();

        resultRepository.save(result);

        if (passed) {
            // Apply Discount
            FarmerSolidarityFund membership = farmerSolidarityFundRepository
                    .findByFarmerIdAndSolidarityFundId(userId, test.getFund().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "You must be a member of the fund to take the test and get a discount."));

            membership.setDiscountPercentage(10.0);
            farmerSolidarityFundRepository.save(membership);
        }

        return result;
    }
}
