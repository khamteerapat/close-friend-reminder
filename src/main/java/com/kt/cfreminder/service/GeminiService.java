package com.kt.cfreminder.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.kt.cfreminder.dto.ReminderAiResponse;
import com.kt.cfreminder.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final GenerateContentConfig REMINDER_RESPONSE_CONFIG = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(Schema.builder()
                    .type(Type.Known.OBJECT)
                    .properties(Map.of(
                            "success", Schema.builder().type(Type.Known.BOOLEAN).build(),
                            "message", Schema.builder().type(Type.Known.STRING).build(),
                            "date", Schema.builder().type(Type.Known.STRING).build(),
                            "time", Schema.builder().type(Type.Known.STRING).build()
                    ))
                    .required("success", "message", "date", "time")
                    .build())
            .build();

    private final Client client;
    private final ObjectMapper objectMapper;

    public String getGeminiResponse(String prompt) {
        try {
            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME,
                    prompt,
                    null
            );
            return response.text();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public Optional<ReminderAiResponse> getReminderResponse(String prompt) {
        try {
            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME,
                    prompt,
                    REMINDER_RESPONSE_CONFIG
            );
            return parseReminderResponse(response.text());
        } catch (Exception e) {
            log.warn("Gemini reminder request failed");
            return Optional.empty();
        }
    }

    public Optional<String> getReminderCommand(String prompt) {
        return getReminderResponse(prompt)
                .filter(response -> Boolean.TRUE.equals(response.success()))
                .map(GeminiService::formatReminderCommand);
    }

    Optional<ReminderAiResponse> parseReminderResponse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode responseNode = objectMapper.readerFor(JsonNode.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(responseText);
            if (!hasResponseContract(responseNode)) {
                return Optional.empty();
            }

            ReminderAiResponse response = objectMapper.treeToValue(responseNode, ReminderAiResponse.class);
            return isValidReminderResponse(response) ? Optional.of(response) : Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Gemini returned an invalid structured reminder response");
            return Optional.empty();
        }
    }

    private static boolean hasResponseContract(JsonNode response) {
        return response != null
                && response.isObject()
                && response.has("success") && response.get("success").isBoolean()
                && response.has("message") && response.get("message").isString()
                && response.has("date") && response.get("date").isString()
                && response.has("time") && response.get("time").isString();
    }

    static String formatReminderCommand(ReminderAiResponse response) {
        LocalDate date = LocalDate.parse(response.date());
        return "เตือน '%s' %s %s".formatted(
                unwrapMessageQuotes(response.message()),
                DateUtils.formatBuddhistDate(date),
                response.time()
        );
    }

    private static String unwrapMessageQuotes(String message) {
        if (message.length() >= 2 && message.startsWith("'") && message.endsWith("'")) {
            return message.substring(1, message.length() - 1);
        }
        return message;
    }

    private static boolean isValidReminderResponse(ReminderAiResponse response) {
        if (response == null || response.success() == null) {
            return false;
        }

        if (!response.success()) {
            return true;
        }

        if (isBlank(response.message()) || isBlank(response.date()) || isBlank(response.time())
                || !ISO_DATE_PATTERN.matcher(response.date()).matches()
                || !TIME_PATTERN.matcher(response.time()).matches()) {
            return false;
        }

        try {
            LocalDate.parse(response.date());
            LocalTime.parse(response.time(), TIME_FORMAT);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
