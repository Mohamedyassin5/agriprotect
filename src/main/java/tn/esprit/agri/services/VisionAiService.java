package tn.esprit.agri.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.agri.controlleurs.assistant.dto.GroqRequest1;
import tn.esprit.agri.controlleurs.assistant.dto.GroqResponse1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VisionAiService {

    private final WebClient webClient;

    @Value("${groq.api.vision-model:meta-llama/llama-4-scout-17b-16e-instruct}")
    private String visionModel;


    @Value("${groq.api.timeout-ms:30000}")
    private long timeoutMs;

    public VisionAiService(
            WebClient.Builder builder,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String baseUrl
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String analyzeSinistreImage(byte[] imageBytes, String contentType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // Détecter le type MIME (par défaut image/jpeg)
        String mimeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;


        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "text",
                "text", "Tu es un expert en sinistres agricoles. Analyse cette image et identifie le type de catastrophe parmi : INCENDIE, INONDATION, SEISME, SECHERESSE, GRESIL, OURAGAN. " +
                        "Réponds uniquement au format JSON suivant : {\"type\": \"TYPE_DETECTE\", \"quota\": POURCENTAGE_FLOTTANT, \"description\": \"BRÈVE_EXPLICATION\"}. " +
                        "Le quota est un pourcentage d'indemnisation (0.0 à 100.0) selon la gravité. Si rien n'est détecté, mets TYPE_DETECTE à 'AUTRE' et quota à 0.0."
        ));
        content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
        ));

        GroqRequest1.GroqMessage message = GroqRequest1.GroqMessage.builder()
                .role("user")
                .content(content)
                .build();

        GroqRequest1 request = GroqRequest1.builder()
                .model(visionModel)
                .messages(List.of(message))
                .temperature(0.1)
                .maxTokens(500)
                .build();

        try {
            GroqResponse1 response = webClient
                    .post()
                    .uri("/chat/completions") // No leading slash to append to baseUrl properly
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse1.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();


            if (response != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Error calling Groq Vision API (Status: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling Groq Vision API: ", e);
        }


        return "{\"type\": \"AUTRE\", \"quota\": 0.0, \"description\": \"Erreur lors de l'analyse IA\"}";
    }
}
