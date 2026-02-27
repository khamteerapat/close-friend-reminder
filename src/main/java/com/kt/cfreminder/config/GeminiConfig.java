package com.kt.cfreminder.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Value("${google.genai.api-key}")
    private String apiKey;

    @Bean
    public Client genAiClient() {
        // สร้าง Client โดยใช้ API Key
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}
