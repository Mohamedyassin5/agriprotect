package tn.esprit.agri.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQcmResponse {
    private String text;
    private List<String> options;
    private String correctAnswer;
}
