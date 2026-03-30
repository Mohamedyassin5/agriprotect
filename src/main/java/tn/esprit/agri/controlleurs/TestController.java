package tn.esprit.agri.controlleurs;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.FarmerQcmResult;
import tn.esprit.agri.entities.QcmQuestion;
import tn.esprit.agri.entities.QcmTest;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.services.TestService;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    // Create Test (Admin only)
    @PostMapping("/create")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTest(@RequestBody CreateTestDTO dto) {
        try {
            QcmTest test = testService.createTestForFund(dto.getFundId(), dto.getTitle(), dto.getRequiredScore(),
                    dto.getQuestions());
            return ResponseEntity.ok(test);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/fund/{fundId}")
    public ResponseEntity<?> getTestByFund(@PathVariable String fundId) {
        try {
            QcmTest test = testService.getTestByFund(fundId);
            if (test == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(test);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{testId}/submit")
    public ResponseEntity<?> submitTest(
            @PathVariable String testId,
            @RequestBody java.util.Map<String, String> answers,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            FarmerQcmResult result = testService.submitTest(user.getId(), testId, answers);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class CreateTestDTO {
        private String fundId;
        private String title;
        private double requiredScore;
        private List<QcmQuestion> questions;
    }
}
