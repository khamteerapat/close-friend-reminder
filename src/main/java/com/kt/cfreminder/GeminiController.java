package com.kt.cfreminder;

import com.kt.cfreminder.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;

    @GetMapping("/chat")
    public String chat(@RequestParam String prompt) {
        return geminiService.getGeminiResponse(prompt);
    }
}
