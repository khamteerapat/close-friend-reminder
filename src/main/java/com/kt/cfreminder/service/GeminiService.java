package com.kt.cfreminder.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {
    private final Client client;


    public String getGeminiResponse(String prompt) {
        try {
            // เรียกใช้โมเดล gemini-2.5-flash
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
            );
            return response.text();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
