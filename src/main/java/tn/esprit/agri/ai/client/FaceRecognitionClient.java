package tn.esprit.agri.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.agri.ai.dto.FaceMatchResponse;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FaceRecognitionClient {

    private final WebClient.Builder builder;

    @Value("${agri.face.base-url:http://localhost:8001}")
    private String baseUrl;

    @Value("${agri.face.timeout-ms:5000}")
    private long timeoutMs;

    public FaceMatchResponse compare(MultipartFile ref, MultipartFile live) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();

        try {
            // We must provide a filename so Python sees it as a file upload
            body.part("file1", new ByteArrayResource(ref.getBytes()) {
                @Override
                public String getFilename() {
                    return ref.getOriginalFilename() != null ? ref.getOriginalFilename() : "ref.jpg";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

            body.part("file2", new ByteArrayResource(live.getBytes()) {
                @Override
                public String getFilename() {
                    return live.getOriginalFilename() != null ? live.getOriginalFilename() : "live.jpg";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

        } catch (IOException e) {
            FaceMatchResponse r = new FaceMatchResponse();
            r.setMatch(false);
            r.setError("Error reading files: " + e.getMessage());
            return r;
        }

        return builder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/compare")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(FaceMatchResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(ex -> {
                    FaceMatchResponse r = new FaceMatchResponse();
                    r.setMatch(false);
                    r.setError("Face service error: " + ex.getMessage());
                    return Mono.just(r);
                })
                .block();
    }
}
