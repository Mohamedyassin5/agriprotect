package tn.esprit.agri.controlleurs.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String answer;
    private String model;
    private Long tokensUsed;
}
