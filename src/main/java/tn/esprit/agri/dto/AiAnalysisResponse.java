package tn.esprit.agri.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {
    private Double confidenceScore;
    private String analysisJustification;
    private String recommendation; // APPROVE, REFUSE, or MANUAL_REVIEW
}
