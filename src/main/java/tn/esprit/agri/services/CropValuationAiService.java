package tn.esprit.agri.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.agri.controlleurs.assistant.dto.GroqRequest;
import tn.esprit.agri.controlleurs.assistant.dto.GroqResponse;
import tn.esprit.agri.entities.Crop;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class CropValuationAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.model:llama3-70b-8192}")
    private String model;

    @Value("${groq.api.timeout-ms:30000}")
    private long timeoutMs;

    public CropValuationAiService(
            WebClient.Builder builder,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Double estimateCropValue(Crop crop) {
        String prompt = String.format(
                "You are an agricultural economist specializing in the Tunisian market. " +
                "Estimate the financial value of a crop based on the following details:\n" +
                "- Crop Type: %s\n" +
                "- Surface Area: %.2f hectares\n" +
                "- Soil Type (Type de terre): %s\n\n" +
                "Use your knowledge of average yields (tons per hectare) and current market prices per ton in Tunisia (in TND). " +
                "The implicit formula is: Estimated Value = Expected Yield * Market Price Per Ton * Surface.\n" +
                "Respond ONLY with a valid JSON object in the following format: {\"estimatedValue\": <numeric_value>}. " +
                "Do not include any other text, markdown formatting, or explanations.",
                crop.getCropType() != null ? crop.getCropType() : "Unknown",
                crop.getSurface() != null ? crop.getSurface() : 1.0f,
                crop.getTypeterres() != null ? crop.getTypeterres() : "Unknown"
        );

        GroqRequest.GroqMessage systemMessage = GroqRequest.GroqMessage.builder()
                .role("system")
                .content("You are a strict JSON-only API that estimates crop values in the Tunisian market.")
                .build();

        GroqRequest.GroqMessage userMessage = GroqRequest.GroqMessage.builder()
                .role("user")
                .content(prompt)
                .build();

        GroqRequest request = GroqRequest.builder()
                .model(model)
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(150)
                .temperature(0.1) // Low temperature for more deterministic output
                .build();

        try {
            GroqResponse response = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent().trim();
                
                // Sometimes the AI might still wrap in markdown ```json ... ``` despite instructions
                if (content.startsWith("```json")) {
                    content = content.replace("```json", "").replace("```", "").trim();
                }

                JsonNode jsonNode = objectMapper.readTree(content);
                if (jsonNode.has("estimatedValue")) {
                    return jsonNode.get("estimatedValue").asDouble();
                } else {
                    log.error("Groq response JSON missing 'estimatedValue' key: {}", content);
                }
            }
        } catch (Exception e) {
            log.error("Error estimating crop value with Groq API: ", e);
        }

        return null; // Return null if estimation fails
    }
}
