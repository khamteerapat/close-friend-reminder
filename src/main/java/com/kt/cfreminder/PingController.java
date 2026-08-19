package com.kt.cfreminder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
public class PingController {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI pingUri;
    private final AtomicBoolean firstPing = new AtomicBoolean(true);

    public PingController(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.pingUri = URI.create(normalizedBaseUrl + "/api/ping");
    }

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }

    @Scheduled(fixedRate = 40000, initialDelay = 40000)
    public void pingApplication() {
        HttpRequest request = HttpRequest.newBuilder(pingUri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (firstPing.compareAndSet(true, false)) {
                log.info("Initial application ping returned status {}", response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (firstPing.compareAndSet(true, false)) {
                log.warn("Initial application ping was interrupted", exception);
            }
        } catch (Exception exception) {
            if (firstPing.compareAndSet(true, false)) {
                log.warn("Initial application ping failed for {}", pingUri, exception);
            }
        }
    }
}