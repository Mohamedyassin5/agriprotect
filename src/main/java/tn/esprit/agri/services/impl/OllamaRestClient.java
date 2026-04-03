package tn.esprit.agri.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

@Component
@Slf4j
public class OllamaRestClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 30s
        factory.setReadTimeout(120000);    // 120s

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .requestFactory(factory)
                .build();

        log.info("OllamaRestClient initialisé avec format=json + temperature=0.0");
    }

    public String generate(String prompt) {
        try {
            log.info("Appel à Ollama avec prompt ({} chars)...", prompt.length());

            Map<String, Object> request = Map.of(
                    "model", "mistral",           // tu peux changer en llama3.2 ou qwen2.5 plus tard
                    "prompt", prompt,
                    "stream", false,
                    "format", "json",             // ← CLEF : force le mode JSON
                    "options", Map.of(
                            "temperature", 0.0,       // très important pour JSON propre
                            "num_predict", 1200
                    )
            );

            long start = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            String rawResponse = (String) response.get("response");

            long duration = System.currentTimeMillis() - start;
            log.info("Réponse Ollama reçue en {} ms ({} chars)", duration, rawResponse.length());

            // Nettoyage robuste de la réponse
            String cleanJson = cleanAiResponse(rawResponse);

            log.debug("JSON nettoyé : {}", cleanJson);
            return cleanJson;

        } catch (RestClientException e) {
            log.error("Erreur appel Ollama", e);
            throw new RuntimeException("Ollama non disponible ou erreur réseau", e);
        }
    }

    /**
     * Nettoie la réponse : supprime markdown, texte avant/après, espaces inutiles
     */
    private String cleanAiResponse(String response) {
        if (response == null) return "{}";

        String cleaned = response.trim();

        // Supprimer les blocs markdown ```json ... ```
        cleaned = cleaned.replaceAll("(?i)```\\s*(json)?", "")
                .replaceAll("```\\s*$", "")
                .trim();

        // Supprimer tout texte avant le premier '{' et après le dernier '}'
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        // Remplacer les sauts de ligne et tabulations inutiles
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }

    public boolean isRunning() {
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Ollama non disponible");
            return false;
        }
    }
}