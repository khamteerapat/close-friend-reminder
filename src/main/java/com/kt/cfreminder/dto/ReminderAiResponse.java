package com.kt.cfreminder.dto;

public record ReminderAiResponse(
        Boolean success,
        String message,
        String date,
        String time
) {
}
