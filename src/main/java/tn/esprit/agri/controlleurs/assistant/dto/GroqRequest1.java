package tn.esprit.agri.controlleurs.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroqRequest1 {

    private String model;
    private List<GroqMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @Data
    @Builder
    public static class GroqMessage {
        private String role;
        private Object content; // Can be String or List of content objects
    }
}

