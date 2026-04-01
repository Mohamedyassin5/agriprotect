package tn.esprit.agri.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tn.esprit.agri.controlleurs.assistant.dto.ChatRequest;
import tn.esprit.agri.controlleurs.assistant.dto.ChatResponse;
import tn.esprit.agri.controlleurs.assistant.dto.GroqRequest;
import tn.esprit.agri.controlleurs.assistant.dto.GroqResponse;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.CropRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssistantService {

    private final WebClient webClient;
    private final CropRepository cropRepository;

    @Value("${groq.api.model:llama3-70b-8192}")
    private String model;

    @Value("${groq.api.max-tokens:1024}")
    private Integer maxTokens;

    @Value("${groq.api.temperature:0.7}")
    private Double temperature;

    @Value("${groq.api.timeout-ms:30000}")
    private long timeoutMs;

    private static final String SYSTEM_PROMPT = """
            Tu es AgriAssistant, un assistant specialise UNIQUEMENT en agriculture.

            TON DOMAINE:
            - Cultures et varietes de plantes
            - Types de sols et fertilite
            - Irrigation et gestion de l'eau
            - Fertilisation et engrais
            - Maladies et ravageurs des plantes
            - Pratiques agricoles durables
            - Impact climatique sur les cultures
            - Recolte et stockage

            REGLES STRICTES:
            1. Reponds UNIQUEMENT aux questions agricoles
            2. Si question hors agriculture, refuse poliment
            3. Recommande un agronome local pour cas complexes
            4. Reponds en francais si question en francais
            5. Donne des conseils pratiques et concrets
            6. Utilise des emojis agricoles
            """;

    private static final List<String> AGRICULTURE_KEYWORDS = List.of(
            "culture", "plante", "sol", "irrigation", "engrais",
            "recolte", "semence", "graine", "ferme", "champ",
            "tomate", "ble", "mais", "riz", "fruit", "legume",
            "maladie", "ravageur", "insecte", "fertilisant",
            "compost", "biologique", "serre", "plantation",
            "arrosage", "agriculture", "agriculteur", "crop",
            "farming", "plant", "harvest", "soil", "water",
            "pest", "disease", "fertilizer", "seed", "grow",
            "azote", "phosphore", "potassium", "ph", "terrain"
    );

    public AssistantService(
            WebClient.Builder builder,
            CropRepository cropRepository,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String baseUrl
    ) {
        this.cropRepository = cropRepository;
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AssistantService initialized with Groq API");
    }

    public ChatResponse chat(User user, ChatRequest request) {
        String message = request.getMessage().trim();
        log.info(
                "Chat from user: {} | message: {}",
                user.getEmail(),
                message.substring(0, Math.min(50, message.length()))
        );

        if (!isAgricultureRelated(message)) {
            log.info("Off-topic blocked for user: {}", user.getEmail());
            return new ChatResponse(
                    "Je suis AgriAssistant, specialise uniquement en agriculture. " +
                            "Je ne peux pas repondre a cette question.\n\n" +
                            "Posez-moi des questions sur:\n" +
                            "- Vos cultures\n" +
                            "- Types de sols\n" +
                            "- Irrigation\n" +
                            "- Maladies des plantes\n" +
                            "- Engrais et fertilisation",
                    model,
                    0L
            );
        }

        String userContext = buildUserContext(user);
        List<GroqRequest.GroqMessage> messages = buildMessages(userContext, request);

        GroqRequest groqRequest = GroqRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        return callGroqApi(groqRequest, user.getEmail());
    }

    private ChatResponse callGroqApi(GroqRequest groqRequest, String userEmail) {
        try {
            GroqResponse response = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(groqRequest)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("Reponse vide de l'IA");
            }

            String answer = response.getChoices().get(0).getMessage().getContent();
            Long tokens = response.getUsage() != null ? response.getUsage().getTotalTokens() : 0L;

            log.info("Groq OK | tokens: {} | user: {}", tokens, userEmail);
            return new ChatResponse(answer, model, tokens);

        } catch (WebClientResponseException e) {
            log.error("Groq error: {} | {}", e.getStatusCode(), e.getResponseBodyAsString());

            String msg = switch (e.getStatusCode().value()) {
                case 429 -> "Limite atteinte. Reessayez dans 1 minute.";
                case 401 -> "Cle API invalide.";
                case 503 -> "Service IA indisponible.";
                default -> "Erreur IA: " + e.getStatusCode();
            };
            throw new RuntimeException(msg);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            throw new RuntimeException("Service indisponible. Reessayez.");
        }
    }

    private String buildUserContext(User user) {
        List<Crop> crops = cropRepository.findByUserId(user.getId());

        String cropsSummary = crops.isEmpty()
                ? "(Aucune culture enregistree)"
                : crops.stream()
                .limit(10)
                .map(c -> String.format(
                        Locale.ROOT,
                        "  - %s | Sol: %s | Temp: %.0f-%.0fC | Humidite: %.0f-%.0f%%",
                        c.getCropType(),
                        c.getTypeterres(),
                        c.getMinTemperature(),
                        c.getMaxTemperature(),
                        c.getMinHumidity(),
                        c.getMaxHumidity()
                ))
                .collect(Collectors.joining("\n"));

        return String.format(
                Locale.ROOT,
                """
                Profil agriculteur:
                - Email: %s
                - Score: %.0f/100

                Cultures actuelles:
                %s
                """,
                user.getEmail(),
                user.getScore() != null ? user.getScore() : 50f,
                cropsSummary
        );
    }

    private List<GroqRequest.GroqMessage> buildMessages(String userContext, ChatRequest request) {
        List<GroqRequest.GroqMessage> messages = new ArrayList<>();

        messages.add(GroqRequest.GroqMessage.builder()
                .role("system")
                .content(SYSTEM_PROMPT + "\n\nCONTEXTE:\n" + userContext)
                .build());

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            request.getHistory().forEach(h -> messages.add(
                    GroqRequest.GroqMessage.builder()
                            .role(h.getRole())
                            .content(h.getContent())
                            .build()
            ));
        }

        messages.add(GroqRequest.GroqMessage.builder()
                .role("user")
                .content(request.getMessage())
                .build());

        return messages;
    }

    private boolean isAgricultureRelated(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return AGRICULTURE_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
