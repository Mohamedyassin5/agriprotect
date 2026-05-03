package tn.esprit.agri.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.agri.ai.dto.FinancialRiskRequest;
import tn.esprit.agri.ai.dto.FinancialRiskResponse;
import tn.esprit.agri.ai.dto.SavingsAlertRequest;
import tn.esprit.agri.ai.dto.SavingsAlertResponse;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class FinanceAIClient {

    private final WebClient.Builder builder;

    @Value("${finance.ai.base-url:http://localhost:8002}")
    private String baseUrl;

    @Value("${finance.ai.timeout-ms:10000}")
    private long timeoutMs;

    public FinancialRiskResponse predictFinancialRisk(FinancialRiskRequest request) {
        return builder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/predict/financial-risk")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FinancialRiskResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(ex -> Mono.error(new RuntimeException("Finance AI service unavailable: " + ex.getMessage())))
                .block();
    }

    public SavingsAlertResponse predictSavingsAlert(SavingsAlertRequest request) {
        return builder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/predict/savings-alert")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SavingsAlertResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(ex -> Mono.error(new RuntimeException("Finance AI service unavailable: " + ex.getMessage())))
                .block();
    }
}
