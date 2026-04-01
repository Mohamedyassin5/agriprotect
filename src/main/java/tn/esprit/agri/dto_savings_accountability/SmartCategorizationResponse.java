package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartCategorizationResponse {

    private String inputDescription;
    private EntryCategory suggestedCategory;
    private double confidenceScore;
    private List<CategoryScore> allScores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryScore {
        private EntryCategory category;
        private double score;
        private List<String> matchedKeywords;
    }
}
