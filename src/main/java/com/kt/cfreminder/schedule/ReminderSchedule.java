package com.kt.cfreminder.schedule;

import com.kt.cfreminder.entity.Reminder;
import com.kt.cfreminder.repository.ReminderRepository;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderSchedule {
    private final ReminderRepository reminderRepository;
    private final MessagingApiClient messagingApiClient;

    @Scheduled(cron = "0 * * * * *") // รันทุกนาที
    @Transactional // ใช้ Transactional เพื่อให้การดึงและอัปเดตสถานะทำงานร่วมกันได้
    public void processReminders() {
        // ดึงเวลาปัจจุบันเป็น UTC เพื่อเทียบกับใน DB
        LocalDateTime nowUtc = LocalDateTime.now(ZoneId.of("UTC"));

        // 1. ดึง Task ที่เป็น PENDING และถึงเวลาแล้ว
        List<Reminder> dueReminders = reminderRepository.findByStatusAndRemindAtBefore("PENDING", nowUtc);

        for (Reminder reminder : dueReminders) {
            try {
                FlexMessage flexMessage = createReminderFlexMessage(reminder);

                // 1. สร้าง Retry Key (UUID) สำหรับข้อความใบนี้
                UUID retryKey = UUID.randomUUID();

                // 2. ปั้น Request Object (ตาม Record Syntax ที่เราดูไปก่อนหน้า)
                PushMessageRequest pushRequest = new PushMessageRequest(
                        reminder.getTargetId(),
                        List.of(flexMessage),
                        false,
                        null
                );

                // 3. เรียก pushMessage พร้อมส่ง retryKey เข้าไปด้วย
                messagingApiClient.pushMessage(retryKey, pushRequest).get(); // .get() เพื่อรอผลแบบ Sync (ใน Scheduler)

                // 4. อัปเดตสถานะ
                reminder.setStatus("SENT");
                reminderRepository.save(reminder);

                log.info("Sent with Retry-Key: {}", retryKey);

            } catch (Exception e) {
                log.error("Failed to push message for reminder: {}", reminder.getId(), e);
            }
        }
    }

    private FlexMessage createReminderFlexMessage(Reminder reminder) {
        String reminderId = reminder.getId().toString();

        // 1. สร้าง FlexText สำหรับส่วนหัว (ใช้ new FlexText.Builder())
        FlexText titleText = new FlexText.Builder()
                .text("🔔 แจ้งเตือนความจำ")
                .weight(FlexText.Weight.BOLD)
                .color("#1DB446")
                .size("sm") // ใช้ String ตาม Record definition
                .build();

        // 2. สร้าง FlexText สำหรับเนื้อหา
        FlexText contentText = new FlexText.Builder()
                .text(reminder.getMessageContent())
                .weight(FlexText.Weight.BOLD)
                .size("xl")
                .wrap(true)
                .margin("md")
                .build();

        // 3. สร้าง FlexBox Body (ใช้ new FlexBox.Builder ที่รับ Layout และ Contents)
        FlexBox body = new FlexBox.Builder(
                FlexBox.Layout.VERTICAL,
                List.of(titleText, contentText)
        ).build();

        // 4. สร้างปุ่ม (FlexButton) - อิงตามแพทเทิร์นเดียวกัน
        PostbackAction doneAction = new PostbackAction("ทำเสร็จแล้ว", "action=done&id=" + reminderId, "ทำเรียบร้อยแล้วครับ", null, null, null);
        FlexButton doneBtn = new FlexButton.Builder(doneAction)
                .style(FlexButton.Style.PRIMARY)
                .color("#1DB446")
                .build();

        // --- ปุ่มเลื่อนเวลา ---
        PostbackAction snoozeAction = new PostbackAction("ค่อยเตือนทีหลัง (15น.)", "action=snooze&id=" + reminderId, "เดี๋ยวมาเตือนใหม่นะ", null, null, null);
        FlexButton snoozeBtn = new FlexButton.Builder(snoozeAction)
                .style(FlexButton.Style.SECONDARY)
                .build();

        // --- ปุ่มยกเลิก ---
        PostbackAction cancelAction = new PostbackAction("ยกเลิกการเตือน", "action=cancel&id=" + reminderId, "ยกเลิกการเตือนนี้แล้ว", null, null, null);
        FlexButton cancelBtn = new FlexButton.Builder(cancelAction)
                .style(FlexButton.Style.LINK)
                .color("#FF5555")
                .build();

        // 5. สร้าง FlexBox Footer
        FlexBox footer = new FlexBox.Builder(
                FlexBox.Layout.VERTICAL,
                List.of(doneBtn, snoozeBtn, cancelBtn)
        ).spacing("sm").build();

        // 6. ประกอบเป็น Bubble
        // ตรวจสอบในไฟล์ FlexBubble.class ถ้า builder() ไม่ทำงาน ให้ใช้ new FlexBubble.Builder()
        FlexBubble bubble = new FlexBubble.Builder()
                .body(body)
                .footer(footer)
                .build();

        return new FlexMessage("คุณมีแจ้งเตือนใหม่: " + reminder.getMessageContent(), bubble);
    }
}
