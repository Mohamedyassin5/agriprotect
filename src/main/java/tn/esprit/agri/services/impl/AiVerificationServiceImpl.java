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

        @Value("${groq.api.vision-model}")
        private String visionModel;

        private final RestTemplate restTemplate;
        private final tn.esprit.agri.repositories.QuestionBankRepository questionBankRepository;
        private final ObjectMapper objectMapper = new ObjectMapper();

        // ─────────────────────────────────────────────────────────────
        //  PUBLIC ENTRY POINT — routes to image or text-only analysis
        // ─────────────────────────────────────────────────────────────
        @Override
        public AiAnalysisResponse verifyClaim(String reason, String cropType, String location, MultipartFile image) {
                try {
                        if (image != null && !image.isEmpty()) {
                                return verifyWithImage(reason, cropType, location, image);
                        } else {
                                return verifyTextOnly(reason, cropType, location);
                        }
                } catch (Exception e) {
                        log.error("AI Service Error: {}", e.getMessage(), e);
                        return AiAnalysisResponse.builder()
                                .confidenceScore(0.0)
                                .analysisJustification("AI Service Error: " + e.getMessage())
                                .recommendation("REFUSE")
                                .build();
                }
        }

        // ─────────────────────────────────────────────────────────────
        //  VISION ANALYSIS (with image)
        // ─────────────────────────────────────────────────────────────
        private AiAnalysisResponse verifyWithImage(String reason, String cropType, String location, MultipartFile image) throws Exception {
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

                Map<String, Object> message = Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", promptText),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:" + mimeType + ";base64," + base64Image))));

                return callGroq(visionModel, message, 800);
        }

        // ─────────────────────────────────────────────────────────────
        //  TEXT-ONLY ANALYSIS (sinistre already visually validated)
        // ─────────────────────────────────────────────────────────────
        private AiAnalysisResponse verifyTextOnly(String reason, String cropType, String location) throws Exception {
                String promptText = String.format(
                        """
                        You are an agricultural insurance claim evaluator.
                        Evaluate the plausibility of the following claim based solely on the provided description.

                        CLAIM DETAILS:
                        - Crop type: %s
                        - Location: %s
                        - Description: %s

                        SCORING (0.0 to 1.0):
                        - 0.9 - 1.0: Highly plausible, detailed and coherent description.
                        - 0.5 - 0.8: Plausible but lacks specific details.
                        - 0.0 - 0.2: Vague, incoherent, or suspicious description.

                        Note: The sinistre was already declared and visually validated at declaration time.
                        This is a text-based plausibility check of the compensation claim.

                        Respond ONLY with a JSON object:
                        {"confidenceScore": 0.0-1.0, "analysisJustification": "Brief justification based on description coherence", "recommendation": "APPROVE or REFUSE or MANUAL_REVIEW"}
                        """,
                        cropType, location, reason);

                Map<String, Object> message = Map.of("role", "user", "content", promptText);
                return callGroq("llama-3.3-70b-versatile", message, 400);
        }

        // ─────────────────────────────────────────────────────────────
        //  SHARED GROQ HTTP CALL
        // ─────────────────────────────────────────────────────────────
        private AiAnalysisResponse callGroq(String model, Map<String, Object> message, int maxTokens) throws Exception {
                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "messages", List.of(message),
                        "max_tokens", maxTokens,
                        "temperature", 0.0);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                log.info("Contacting Groq [model={}] for claim analysis...", model);

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

                log.info("Groq Raw Response: {}", content);

                String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
                return objectMapper.readValue(json, AiAnalysisResponse.class);
        }

        // ─────────────────────────────────────────────────────────────
        //  QCM GENERATION
        // ─────────────────────────────────────────────────────────────
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

                        Map<String, Object> message = Map.of("role", "user", "content", promptText);

                        Map<String, Object> requestBody = Map.of(
                                "model", "llama-3.3-70b-versatile",
                                "messages", List.of(message),
                                "max_tokens", 1500,
                                "temperature", 0.7);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(groqApiKey);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                        log.info("Contacting Groq for QCM generation... crop={}, keyLength={}", cultureType,
                                (groqApiKey != null ? groqApiKey.length() : 0));

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
                        if (json.contains("[")) {
                                json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
                        }

                        return objectMapper.readValue(json,
                                new com.fasterxml.jackson.core.type.TypeReference<List<tn.esprit.agri.DTO.AiQcmResponse>>() {});

                } catch (Exception e) {
                        log.error("AI QCM Generation Error: {}. Using fallback mechanism.", e.getMessage());

                        return List.of(
                                tn.esprit.agri.DTO.AiQcmResponse.builder()
                                        .text("Quelle est la période optimale pour la gestion intensive de la main-d'œuvre pour la culture de " + cultureType + " ?")
                                        .options(List.of("Phase de semis", "Phase de croissance végétative", "Période de récolte", "Période de repos hivernal"))
                                        .correctAnswer("Période de récolte")
                                        .build(),
                                tn.esprit.agri.DTO.AiQcmResponse.builder()
                                        .text("Quel facteur environnemental influence le plus le besoin en travailleurs saisonniers pour le " + cultureType + " ?")
                                        .options(List.of("La couleur du sol", "Les conditions météorologiques (pluie/soleil)", "La proximité des routes", "Le type de clôture"))
                                        .correctAnswer("Les conditions météorologiques (pluie/soleil)")
                                        .build(),
                                tn.esprit.agri.DTO.AiQcmResponse.builder()
                                        .text("Quelle technique permet d'optimiser l'efficacité de la main-d'œuvre lors de l'entretien du " + cultureType + " ?")
                                        .options(List.of("Le travail manuel sans outils", "La mécanisation partielle et l'organisation en équipes", "Le travail de nuit uniquement", "L'absence de supervision"))
                                        .correctAnswer("La mécanisation partielle et l'organisation en équipes")
                                        .build(),
                                tn.esprit.agri.DTO.AiQcmResponse.builder()
                                        .text("Comment prévenir les risques d'accidents pour les ouvriers travaillant sur le " + cultureType + " ?")
                                        .options(List.of("Porter des vêtements de ville", "Utiliser des équipements de protection individuelle (EPI)", "Travailler plus vite", "Ignorer les consignes de sécurité"))
                                        .correctAnswer("Utiliser des équipements de protection individuelle (EPI)")
                                        .build(),
                                tn.esprit.agri.DTO.AiQcmResponse.builder()
                                        .text("Quel est le signe principal indiquant un besoin urgent de main-d'œuvre pour la récolte du " + cultureType + " ?")
                                        .options(List.of("La maturité physiologique des fruits/grains", "Le début de l'hiver", "La fin du contrat des ouvriers", "L'arrivée de nouveaux outils"))
                                        .correctAnswer("La maturité physiologique des fruits/grains")
                                        .build()
                        );
                }
        }

        // ─────────────────────────────────────────────────────────────
        //  AI QUESTION BANK LOGIC (SMART SELF-FEEDING)
        // ─────────────────────────────────────────────────────────────
        @Override
        public List<tn.esprit.agri.entities.QuestionBank> getRandomQuestionsFromBank(String cultureType, int count) {
                long currentCount = questionBankRepository.countByCropType(cultureType);

                // If the bank is running low on questions for this crop type, feed it asynchronously
                if (currentCount < 20) {
                        log.info("Question bank for {} is low ({} questions). Triggering background AI feeding.", cultureType, currentCount);
                        java.util.concurrent.CompletableFuture.runAsync(() -> feedQuestionBankAsync(cultureType));
                }

                // If we don't even have enough questions to serve the user right now, generate them synchronously
                if (currentCount < count) {
                        log.info("Not enough questions in bank for {}. Synchronously generating {} questions.", cultureType, count);
                        // Generate 5 questions (this is using the existing generateQcm method which returns DTOs)
                        List<tn.esprit.agri.DTO.AiQcmResponse> dtos = generateQcm(cultureType);

                        // Save them to the bank
                        List<tn.esprit.agri.entities.QuestionBank> newQuestions = dtos.stream().map(dto ->
                                tn.esprit.agri.entities.QuestionBank.builder()
                                        .cropType(cultureType)
                                        .text(dto.getText())
                                        .options(new java.util.ArrayList<>(dto.getOptions()))
                                        .correctAnswer(dto.getCorrectAnswer())
                                        .build()
                        ).toList();

                        questionBankRepository.saveAll(newQuestions);
                        return newQuestions.subList(0, Math.min(count, newQuestions.size()));
                }

                // Normal flow: pull random questions directly from the database (instantaneous)
                return questionBankRepository.findRandomByCropType(cultureType, count);
        }

        @Override
        public void feedQuestionBankAsync(String cultureType) {
                try {
                        log.info("Starting async background AI generation for {} question bank...", cultureType);

                        // Let's ask AI to generate 10 more questions at once
                        // We use a modified prompt to get 10 questions
                        String promptText = String.format(
                                """
                                Tu es un expert agronome.
                                Génère 10 NOUVELLES questions de QCM technique en Français sur la culture du "%s".
                                Assure-toi que ces questions sont originales, d'un niveau professionnel, et portent sur la gestion, les maladies, ou la main d'œuvre.
                                FORMAT : 10 questions, 4 options, 1 seule réponse correcte (copie exacte d'une option).
                                SORTIE : Réponds UNIQUEMENT par un tableau JSON valide.
                                [
                                  {
                                    "text": "Question sur le %s ?",
                                    "options": ["...", "...", "...", "..."],
                                    "correctAnswer": "..."
                                  }
                                ]
                                """, cultureType, cultureType);

                        Map<String, Object> message = Map.of("role", "user", "content", promptText);
                        Map<String, Object> requestBody = Map.of(
                                "model", "llama-3.3-70b-versatile",
                                "messages", List.of(message),
                                "max_tokens", 2500,
                                "temperature", 0.9); // higher temperature for more variety

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(groqApiKey);
                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                "https://api.groq.com/openai/v1/chat/completions",
                                org.springframework.http.HttpMethod.POST,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {});

                        Map<String, Object> responseBody = response.getBody();
                        if (responseBody != null) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                                @SuppressWarnings("unchecked")
                                Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
                                String content = (String) messageResponse.get("content");

                                String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
                                if (json.contains("[")) {
                                        json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
                                }

                                List<tn.esprit.agri.DTO.AiQcmResponse> dtos = objectMapper.readValue(json,
                                        new com.fasterxml.jackson.core.type.TypeReference<List<tn.esprit.agri.DTO.AiQcmResponse>>() {});

                                List<tn.esprit.agri.entities.QuestionBank> newQuestions = dtos.stream().map(dto ->
                                        tn.esprit.agri.entities.QuestionBank.builder()
                                                .cropType(cultureType)
                                                .text(dto.getText())
                                                .options(new java.util.ArrayList<>(dto.getOptions()))
                                                .correctAnswer(dto.getCorrectAnswer())
                                                .build()
                                ).toList();

                                questionBankRepository.saveAll(newQuestions);
                                log.info("Successfully added {} new questions to the {} question bank in background.", newQuestions.size(), cultureType);
                        }
                } catch (Exception e) {
                        log.error("Failed to feed question bank in background for {}: {}", cultureType, e.getMessage());
                }
        }
}