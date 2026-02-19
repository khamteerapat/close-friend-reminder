package com.kt.cfreminder.service;

import com.kt.cfreminder.entity.Reminder;
import com.kt.cfreminder.enums.LineBotCommand;
import com.kt.cfreminder.enums.ReminderTaskStatus;
import com.kt.cfreminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository reminderRepository;

    public void saveReminder(String senderId, String targetId, String content) {
        // 1. ล้างคำสั่งออก (จำ จ่ายค่าประกัน 23/03/2569 14:45 -> จ่ายค่าประกัน 23/03/2569 14:45)
        String cleanContent = content.replaceFirst("(?i)" + LineBotCommand.REMIND.getThaiCommand(), "").trim();

        // 2. Regex สำหรับดึงวันเวลา (DD/MM/YYYY HH:mm)
        Pattern pattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(\\d{1,2}):(\\d{1,2})");
        Matcher matcher = pattern.matcher(cleanContent);

        if (matcher.find()) {
            LocalDateTime localDateTime = getLocalDateTime(matcher);
            ZonedDateTime thaiTime = localDateTime.atZone(ZoneId.of("Asia/Bangkok"));
            ZonedDateTime utcTime = thaiTime.withZoneSameInstant(ZoneId.of("UTC"));

            // ข้อความที่จะใช้เตือน (ตัดส่วนวันเวลาออก)
            String message = cleanContent.replace(matcher.group(0), "").trim();
            if (message.isEmpty()) message = "แจ้งเตือนจ้า!";

            // 4. บันทึกลง DB
            Reminder reminder = Reminder.builder()
                    .senderId(senderId)
                    .targetId(targetId)
                    .messageContent(message)
                    .remindAt(utcTime.toLocalDateTime()) // บันทึกเป็น UTC
                    .status(ReminderTaskStatus.PENDING.name())
                    .build();

            reminder.setCreatedBy("SYSTEM");
            reminder.setCreatedAt(Instant.now());

            reminderRepository.save(reminder);
        } else {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    @NotNull
    private static LocalDateTime getLocalDateTime(Matcher matcher) {
        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int year = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));

        // แปลง พ.ศ. -> ค.ศ.
        if (year > 2500) year -= 543;

        // 3. จัดการเรื่อง Timezone: ไทย (GMT+7) -> UTC
        return LocalDateTime.of(year, month, day, hour, minute);
    }
}
