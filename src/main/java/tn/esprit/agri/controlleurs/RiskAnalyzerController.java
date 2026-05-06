package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/agri/risk-analyzer")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class RiskAnalyzerController {

    private final WebClient.Builder webClientBuilder;
    private static final String RISK_SERVICE_URL = "http://localhost:8002";

    @PostMapping("/predict")
    public Mono<ResponseEntity<Map>> predict(@RequestBody Map<String, Object> request) {
        log.info("Receiving prediction request for model: {}", request.get("model"));
        return webClientBuilder.build()
                .post()
                .uri(RISK_SERVICE_URL + "/predict")
                .bodyValue(request)
                .retrieve()
                .toEntity(Map.class)
                .doOnSuccess(res -> log.info("Prediction successful: {}", res.getStatusCode()))
                .doOnError(e -> log.error("Error calling AI service: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    @GetMapping("/test")
    public String test() {
        log.info("Test endpoint reached");
        return "Controller is active";
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map>> health() {
        return webClientBuilder.build()
                .get()
                .uri(RISK_SERVICE_URL + "/health")
                .retrieve()
                .toEntity(Map.class)
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().build()));
    }
}
