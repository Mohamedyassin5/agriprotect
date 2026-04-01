package tn.esprit.agri.DTO.CreditDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioKpiDto {
    private Double totalOutstanding;
    private Double par30Ratio;
    private Double defaultRatioProxy;
    private Double collectionRate;
}
