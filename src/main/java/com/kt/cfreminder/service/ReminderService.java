package com.kt.cfreminder.service;

import com.kt.cfreminder.entity.Reminder;
import com.kt.cfreminder.enums.LineBotCommand;
import com.kt.cfreminder.enums.ReminderTaskStatus;
import com.kt.cfreminder.repository.ReminderRepository;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final MessagingApiClient messagingApiClient;

    public void saveReminder(String senderId, String targetId, String content) {
        // 1. Regex ใหม่:
        // กลุ่ม 1: \"([^\"]+)\" -> จับข้อความที่อยู่ในเครื่องหมายคำพูด
        // กลุ่ม 2: (\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}) -> จับรูปแบบวันเวลา
        Pattern pattern = Pattern.compile("'([^']+)'\\s+(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(\\d{1,2}):(\\d{1,2})");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            // ดึงข้อความจากกลุ่มที่ 1 (ในเครื่องหมายคำพูด)
            String message = matcher.group(1).trim();

            // ดึงวันเวลาจากกลุ่มที่ 2

            // สร้าง Pattern ย่อยเพื่อ Parse วันเวลา (หรือส่งตัวแปร dateTimeStr ไปที่ getLocalDateTime)
            LocalDateTime localDateTime = getLocalDateTime(matcher);

            ZonedDateTime thaiTime = localDateTime.atZone(ZoneId.of("Asia/Bangkok"));
            ZonedDateTime utcTime = thaiTime.withZoneSameInstant(ZoneId.of("UTC"));

            // 2. บันทึกลง DB
            Reminder reminder = Reminder.builder()
                    .senderId(senderId)
                    .targetId(targetId)
                    .messageContent(message)
                    .snoozeCount(0)
                    .remindAt(utcTime.toLocalDateTime())
                    .status(ReminderTaskStatus.PENDING.name())
                    .build();

            reminder.setCreatedBy("SYSTEM");
            reminder.setCreatedAt(Instant.now());

            reminderRepository.save(reminder);
        } else {
            // กรณีรูปแบบไม่ตรง เช่น ลืมใส่เครื่องหมายคำพูด หรือลืมใส่วันที่
            throw new IllegalArgumentException("รูปแบบคำสั่งไม่ถูกต้อง กรุณาใช้: เตือน \"ข้อความ\" DD/MM/YYYY HH:mm");
        }
    }

    public void updateStatus(UUID reminderId, String replyToken, String action){
        reminderRepository.findById(reminderId).ifPresent(reminder -> {
            if(reminder.getStatus().equals(ReminderTaskStatus.SENT.name())){
                switch (action) {
                    case "done" -> {
                        reminder.setStatus(ReminderTaskStatus.DONE.name());
                        reminder.setUpdatedBy("USER_DONE");
                    }
                    case "cancel" -> {
                        reminder.setStatus(ReminderTaskStatus.CANCEL.name());
                        reminder.setUpdatedBy("USER_CANCEL");
                    }
                    case "snooze" -> {
                        // เลื่อนไปอีก 1 ชม จาก "เวลาปัจจุบัน" และเปลี่ยนกลับเป็น PENDING
                        LocalDateTime newTime = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(60);
                        reminder.setRemindAt(newTime);
                        reminder.setStatus(ReminderTaskStatus.PENDING.name());
                        reminder.setSnoozeCount(reminder.getSnoozeCount() + 1);
                        reminder.setUpdatedBy("USER_SNOOZE");
                    }
                }
                reminderRepository.save(reminder);
                // ส่งข้อความยืนยันสั้นๆ (ไม่จำเป็นต้องยาว เพราะ User เห็น displayText จากปุ่มแล้ว)
                messagingApiClient.replyMessage(new ReplyMessageRequest(
                        replyToken,
                        List.of(new TextMessage("ระบบดำเนินการอัปเดตสถานะให้แล้วครับ")),
                        false
                ));
            }else{
                // ส่งข้อความยืนยันสั้นๆ (ไม่จำเป็นต้องยาว เพราะ User เห็น displayText จากปุ่มแล้ว)
                String replyStatus = "คุณ \"%S\" เรียบร้อยแล้วนะครับ";
                messagingApiClient.replyMessage(new ReplyMessageRequest(
                        replyToken,
                        List.of(new TextMessage(String.format(replyStatus,replaceStatusWord(reminder.getStatus())))),
                        false
                ));
            }

        });


    }

    private String replaceStatusWord(String status){
        String statusWord = "";
        switch (status){
            case "DONE" -> statusWord = "ทำเสร็จ";
            case "CANCEL" -> statusWord = "ยกเลิกแจ้งเตือน";
            case "PENDING" -> statusWord = "เลื่อนการเตือน";
        }
        return statusWord;
    }

    @NotNull
    private static LocalDateTime getLocalDateTime(Matcher matcher) {
        // หมายเหตุ: matcher.group(1) คือ "ข้อความในเครื่องหมายคำพูด"
        // ดังนั้น วัน/เดือน/ปี จะเริ่มที่ group 2 เป็นต้นไป

        // Pattern ใหม่: "([^"]+)"\s+(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{1,2})
        int day    = Integer.parseInt(matcher.group(2));
        int month  = Integer.parseInt(matcher.group(3));
        int year   = Integer.parseInt(matcher.group(4));
        int hour   = Integer.parseInt(matcher.group(5));
        int minute = Integer.parseInt(matcher.group(6));

        // แปลง พ.ศ. -> ค.ศ. (กันพลาดเผื่อคนกรอกปี ค.ศ. มาอยู่แล้ว)
        if (year > 2500) {
            year -= 543;
        }

        return LocalDateTime.of(year, month, day, hour, minute);
    }

}
