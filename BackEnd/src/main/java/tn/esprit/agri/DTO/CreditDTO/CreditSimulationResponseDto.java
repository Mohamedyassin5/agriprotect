package tn.esprit.agri.DTO.CreditDTO;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreditSimulationResponseDto {
    private String criteria;
    private List<CreditSimulationResultDto> rankedOffers;
}
