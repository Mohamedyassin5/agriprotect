package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalProgressResponse {

    private Long accountId;
    private String goalTitle;
    private BigDecimal goalAmount;
    private BigDecimal currentBalance;
    private BigDecimal remaining;
    private Double progressPercentage;
    private String status;
}
