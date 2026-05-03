package tn.esprit.agri.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.agri.DTO.AiAnalysisResponse;
import tn.esprit.agri.services.AiVerificationService;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiVerificationServiceImpl implements AiVerificationService {

        @Value("${groq.api.key}")
        private String groqApiKey;

        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public AiAnalysisResponse verifyClaim(String reason, String cropType, String location, MultipartFile image) {
                try {
                        // Convert image to base64
                        byte[] imageBytes = image.getBytes();
                        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

                        String promptText = String.format(
                                        """
                                                        You are a STRICT agricultural insurance fraud detector.
                                                        Your task is to verify if the provided image matches the claim details.

                                                        CLAIM DETAILS:
                                                        - Insured Crop: %s
                                                        - Location: %s
                                                        - Reported Reason: %s

                                                        VERIFICATION RULES:
                                                        1. CROP MATCH: Does the image clearly show %s? If it shows a different crop or no plants at all, FAIL.
                                                        2. DAMAGE CONSISTENCY: Does the image show damage consistent with "%s"? If the plants look healthy or the damage is unrelated, FAIL.
                                                        3. IMAGE RELEVANCY: Is this a real photo of a farm? If it is a stock photo, a meme, an indoor photo, or any irrelevant "garbage" image, FAIL.

                                                        SCORING:
                                                        - 0.9 - 1.0: Everything matches perfectly (Image clearly shows the crop AND damage).
                                                        - 0.5 - 0.8: Possibly related but unclear or low quality.
                                                        - 0.0 - 0.2: IRRELEVANT, WRONG CROP, or SUSPECTED FRAUD.

                                                        CRITICAL: If the image is "garbage", an unrelated object, or a blank screen, you MUST score it 0.0 even if the text description is very professional.

                                                        Respond ONLY with a JSON object:
                                                        {"confidenceScore": 0.0-1.0, "analysisJustification": "Explicitly state what you see in the image and why it matches/mismatches", "recommendation": "APPROVE or REFUSE or MANUAL_REVIEW"}
                                                        """,
                                        cropType, location, reason, cropType, reason);

                        // Build multimodal message with image + text
                        Map<String, Object> message = Map.of(
                                        "role", "user",
                                        "content", List.of(
                                                        Map.of("type", "text", "text", promptText),
                                                        Map.of("type", "image_url", "image_url",
                                                                        Map.of("url", "data:" + mimeType + ";base64,"
                                                                                        + base64Image))));

                        Map<String, Object> requestBody = Map.of(
                                        "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                                        "messages", List.of(message),
                                        "max_tokens", 800,
                                        "temperature", 0.0);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(groqApiKey);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                        log.info("Contacting Groq for claim analysis... crop={}, location={}", cropType, location);

                        // Fetch response from Groq using proper type reference
                        ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {
                        };
                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                        "https://api.groq.com/openai/v1/chat/completions",
                                        org.springframework.http.HttpMethod.POST,
                                        entity,
                                        typeRef);

                        Map<String, Object> responseBody = response.getBody();
                        if (responseBody == null) {
                                throw new RuntimeException("Empty response from AI service");
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
                        String content = (String) messageResponse.get("content");

                        log.info("Groq Raw Response: {}", content);

                        String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
                        return objectMapper.readValue(json, AiAnalysisResponse.class);

                } catch (Exception e) {
                        log.error("AI Service Error: {}", e.getMessage(), e);

                        return AiAnalysisResponse.builder()
                                        .confidenceScore(0.0)
                                        .analysisJustification("AI error: automatically refused for safety.")
                                        .recommendation("REFUSE") // ✅ no more MANUAL_REVIEW
                                        .build();
                }
        }

        @Override
        public List<tn.esprit.agri.DTO.AiQcmResponse> generateQcm(String cultureType) {
                try {
                        String promptText = String.format(
                                        """
                                        Tu es un expert agronome spécialisé dans la gestion de la main-d'œuvre. 
                                        Ton objectif est de générer un QCM technique de 5 questions en Français concernant EXCLUSIVEMENT la culture du "%s".
                                        
                                        CONSIGNES DE RÉDACTION :
                                        1. FOCUS TECHNIQUE : Les questions doivent porter sur les besoins spécifiques en main-d'œuvre, les périodes de récolte, de semis, et les techniques culturales propres au "%s".
                                        2. INTERDICTION : Ne mentionne JAMAIS d'autres cultures (ex: pas de maïs ou soja si on parle de blé). 
                                        3. DIFFICULTÉ : Les questions doivent être sérieuses et utiles pour vérifier l'expertise d'un agriculteur.
                                        4. FORMAT : Exactement 5 questions, 4 options par question, 1 seule réponse correcte.
                                        
                                        RÉPONSES : La "correctAnswer" doit être une copie exacte de l'une des options.

                                        SORTIE : Réponds UNIQUEMENT par un tableau JSON valide.
                                        [
                                          {
                                            "text": "Question sur le %s ?",
                                            "options": ["...", "...", "...", "..."],
                                            "correctAnswer": "..."
                                          }
                                        ]
                                        """, cultureType, cultureType, cultureType);

                        Map<String, Object> message = Map.of(
                                        "role", "user",
                                        "content", promptText);

                        Map<String, Object> requestBody = Map.of(
                                        "model", "llama-3.3-70b-versatile",
                                        "messages", List.of(message),
                                        "max_tokens", 1500,
                                        "temperature", 0.7);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(groqApiKey);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                        log.info("Contacting Groq for QCM generation... crop={}, keyLength={}", 
                                cultureType, (groqApiKey != null ? groqApiKey.length() : 0));

                        ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};
                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                        "https://api.groq.com/openai/v1/chat/completions",
                                        org.springframework.http.HttpMethod.POST,
                                        entity,
                                        typeRef);

                        Map<String, Object> responseBody = response.getBody();
                        if (responseBody == null) {
                                throw new RuntimeException("Empty response from AI service");
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
                        String content = (String) messageResponse.get("content");

                        log.info("Groq Raw QCM Response: {}", content);

                        String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
                        // Sometimes the LLM starts with some text before the array
                        if (json.contains("[")) {
                            json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
                        }

                        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<tn.esprit.agri.DTO.AiQcmResponse>>() {});

                } catch (Exception e) {
                        log.error("AI QCM Generation Error: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to generate QCM via AI", e);
                }
        }
}