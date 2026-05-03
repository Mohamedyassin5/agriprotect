package tn.esprit.agri.controlleurs.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotBlank(message = "Message requis")
    @Size(
            min = 2,
            max = 1000,
            message = "Message entre 2 et 1000 caracteres"
    )
    private String message;

    private List<MessageHistory> history;

    @Data
    public static class MessageHistory {
        private String role;
        private String content;
    }
}
