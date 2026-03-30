package tn.esprit.agri.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.agri.ai.dto.CropAiRequest;
import tn.esprit.agri.ai.dto.CropAiResponse;

import java.time.Duration;

@Component @RequiredArgsConstructor
public class CropAiClient {

    private final WebClient.Builder builder;

    @Value("${agri.ai.base-url}")
    private String baseUrl;

    @Value("${agri.ai.timeout-ms:3000}")
    private long timeoutMs;

    public CropAiResponse predict(CropAiRequest request) {
        return builder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CropAiResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(ex -> Mono.error(new RuntimeException("AI service unavailable: " + ex.getMessage())))
                .block();
    }
}
