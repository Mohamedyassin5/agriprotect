package tn.esprit.agri.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolidarityFundResponse {

    private String id;
    private String name;
    private String numeroFond;
    private String createdByUsername;
}
