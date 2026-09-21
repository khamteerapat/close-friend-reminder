package com.kt.cfreminder.service;

import com.kt.cfreminder.dto.ReminderAiResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiServiceTest {

    private final GeminiService geminiService = new GeminiService(null, new ObjectMapper());

    @Test
    void parsesValidGeminiJsonResponse() {
        Optional<ReminderAiResponse> response = geminiService.parseReminderResponse("""
                {"success":true,"message":"กินยา","date":"2026-09-21","time":"17:47"}
                """);

        assertTrue(response.isPresent());
        assertEquals(new ReminderAiResponse(true, "กินยา", "2026-09-21", "17:47"), response.get());
    }

    @Test
    void rejectsInvalidJsonResponse() {
        assertTrue(geminiService.parseReminderResponse("{invalid").isEmpty());
    }

    @Test
    void rejectsResponseWithMissingRequiredField() {
        assertTrue(geminiService.parseReminderResponse("""
                {"success":true,"message":"กินยา","date":"2026-09-21"}
                """).isEmpty());
    }

    @Test
    void rejectsResponseWithoutSuccess() {
        assertTrue(geminiService.parseReminderResponse("""
                {"message":"กินยา","date":"2026-09-21","time":"17:47"}
                """).isEmpty());
    }

    @Test
    void acceptsFailureResponseWithoutCreatingReminderData() {
        Optional<ReminderAiResponse> response = geminiService.parseReminderResponse("""
                {"success":false,"message":"","date":"","time":""}
                """);

        assertTrue(response.isPresent());
        assertFalse(response.get().success());
    }

    @Test
    void rejectsInvalidCalendarDate() {
        assertTrue(geminiService.parseReminderResponse("""
                {"success":true,"message":"กินยา","date":"2025-02-29","time":"17:47"}
                """).isEmpty());
    }

    @Test
    void rejectsInvalidTime() {
        assertTrue(geminiService.parseReminderResponse("""
                {"success":true,"message":"กินยา","date":"2026-09-21","time":"24:00"}
                """).isEmpty());
    }

    @Test
    void formatsGregorianDateWithBuddhistYear() {
        String command = GeminiService.formatReminderCommand(
                new ReminderAiResponse(true, "กินยา", "2026-09-21", "17:47")
        );

        assertEquals("เตือน 'กินยา' 21/09/2569 17:47", command);
    }

    @Test
    void preservesSingleQuotesInReminderMessage() {
        String command = GeminiService.formatReminderCommand(
                new ReminderAiResponse(true, "'โทรหาแม่'", "2026-09-21", "17:47")
        );

        assertEquals("เตือน 'โทรหาแม่' 21/09/2569 17:47", command);
    }

    @Test
    void preservesExistingDownstreamCommandFormat() {
        String command = GeminiService.formatReminderCommand(
                new ReminderAiResponse(true, "กินยา", "2026-09-21", "17:47")
        );

        assertTrue(command.matches("เตือน '[^']+' \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"));
        assertEquals("เตือน 'กินยา' 21/09/2569 17:47", command);
    }
}
