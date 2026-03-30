package tn.esprit.agri.ai.dto;

import lombok.Data;

@Data
public class FaceMatchResponse {
    private boolean match;
    private Double distance;
    private String error;
}
