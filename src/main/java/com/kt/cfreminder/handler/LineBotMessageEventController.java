package com.kt.cfreminder.handler;

import com.kt.cfreminder.entity.FollowedUser;
import com.kt.cfreminder.enums.FollowedUserStatus;
import com.kt.cfreminder.enums.LineBotCommand;
import com.kt.cfreminder.repository.FollowedUserRepository;
import com.kt.cfreminder.service.GeminiService;
import com.kt.cfreminder.service.ReminderService;
import com.kt.cfreminder.utils.DateUtils;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.*;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.PostbackEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Slf4j
@LineMessageHandler
@RequiredArgsConstructor
public class LineBotMessageEventController {

    private static final String PROMPT_TEMPLATE = """
            มีระบบช่วยเตือนความจำ ที่จะรับ Command ชุดหนึ่งไปประมวลผล
            โดยคำสั่งจะเป็น format แบบนี้
            
            "เตือน <'ข้อความเตือน'> <วันที่ dd/MM/ปี พ.ศ.> <เวลา 24hh:mm GMT+7>"
            
            อยากให้รับข้อมูลจาก user ที่ทำการพิมพ์ข้อความที่ไม่ตรง format มาปรับให้เป็น command ให้ถูกต้อง
            โดยมีตัวอย่างดังนี้
            
            [ตัวอย่างที่ 1] สรุปใจความให้ถูกต้องเป็น command
            User : "เตือน ให้ไปต่อประกันรถวันที่ 24 กุมภา 2569 8 โมงเช้า"
            คำตอบที่ควรตอบกลับ : "เตือน 'ต่อประกันรถ' 24/02/2569 08:00"
            
            [ตัวอย่างที่ 2] ถ้า user มี single quote ควรใส่มาทั้งข้อความ
            User : "เตือน 'อย่าลืมกินข้าวนะจ๊ะ <3' 24 กุมภา 2569 8 โมงเช้า"
            คำตอบที่ควรตอบกลับ : "เตือน 'อย่าลืมกินข้าวนะจ๊ะ <3' 24/02/2569 08:00"
            
            [ตัวอย่างที่ 3] ยังพอเดาช่วงเวลาที่แน่ชัดได้ <วันที่ปัจจุบัน + 1> เวลาราชการ 08:30
            User : "ช่วยเตือนหน่อยพรุ่งนี้ต้องไปติดต่อราชการ"
            คำตอบที่ควรได้ : "เตือน 'ติดต่อราชการ' 25/02/2569 08:00" **สมมติวันที่ปัจจุบันคือ 24/02/2569
            
            [ตัวอย่างที่ 4] ไม่สามารถวิเคราะห์ได้ ขาด context ขาดเวลาที่แน่ชัด
            User : "อย่าลืมเตือนด้วยนะพรุ่งนี้"
            คำตอบที่ควรได้ : "Prompt Error"
            
            เนื่องจากใช้เชื่อมต่อกับ api springboot และรับ response จาก text โดยตรง จึงอยากให้ตอบกลับมาแค่ command เท่านั้น โดยไม่ต้องมี double quote ตอนตอบกลับ
            
            นี่คือข้อความจาก user และขอกำหนดให้เวลาปัจจุบันคือ %s
            
            User : "%s"
            
            """;

    @Value("${app.timezone}")
    private String timezone;

    private final MessagingApiClient messagingApiClient;
    private final ReminderService reminderService;
    private final FollowedUserRepository followedUserRepository;
    private final GeminiService geminiService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent event) {
        log.info("event: {}", event);

        MessageContent messageContent = event.message();


        if (messageContent instanceof TextMessageContent textContent) {

            String originalText = textContent.text();
            String replyToken = event.replyToken();

            String promptString = String.format(PROMPT_TEMPLATE, DateUtils.getCurrentDateTimeThai(timezone), originalText);

            log.info("Prompt Request : {}", promptString );

            String command = geminiService.getGeminiResponse(promptString);

            log.info("Gemini Response : {}", command);

            if (command != null && command.startsWith(LineBotCommand.REMIND.getThaiCommand())) {
                List<FollowedUser> allUsers = followedUserRepository.findByStatus(FollowedUserStatus.FOLLOW.name());

                List<QuickReplyItem> items = allUsers.stream().map(user -> {
                    // สร้าง Data สำหรับส่งกลับมาตอนกดปุ่ม (ต้องไม่เกิน 300 ตัวอักษร)
                    // แนะนำให้ส่ง userId ของคนที่จะถูกเตือนกลับมาด้วย
                    String postbackData = String.format("action=save&targetId=%s&content=%s",
                            user.getUserId(), command);

                    return new QuickReplyItem(null,
                            new PostbackAction(
                                    user.getDisplayName(), // ชื่อที่แสดงบนปุ่ม (Label)
                                    postbackData,          // ข้อมูลที่จะได้รับใน handlePostback
                                    "เลือกเตือนคุณ " + user.getDisplayName(), // ข้อความที่จะเด้งในแชทเมื่อกด
                                    null, null, null
                            )
                    );
                }).toList();


                // 3. สร้าง QuickReply object
                QuickReply quickReply = new QuickReply(items);

                // 4. สร้างและส่ง TextMessage
                TextMessage textMessage = new TextMessage.Builder("รับทราบครับ! แล้วจะให้เตือนใครดี?")
                        .quickReply(quickReply)
                        .build();

                messagingApiClient.replyMessage(new ReplyMessageRequest(
                        replyToken,
                        List.of(textMessage),
                        false
                ));


            } else if (command != null && command.contains("Prompt Error")) {
                TextMessage textMessage = new TextMessage.Builder("รายละเอียดไม่ค่อยชัดเจน โปรดระบุให้ชัดเจนอีกหน่อยครับ")
                        .build();

                messagingApiClient.replyMessage(new ReplyMessageRequest(
                        replyToken,
                        List.of(textMessage),
                        false
                ));
            }
        }


    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        log.info("Postback event: {}", event);
        String data = event.postback().data();
        String replyToken = event.replyToken();
        Map<String, String> params = parseQueryParams(data);

        // ตรวจสอบว่ามี Key 'action' หรือไม่
        String action = params.get("action");
        if (action == null) return;

        switch (action) {
            case "save" -> {
                // Logic สำหรับการบันทึก Reminder ใหม่ (จาก Quick Reply)
                handleSaveReminderAction(event, params, replyToken);
            }
            case "done", "snooze", "cancel" -> {
                // Logic สำหรับการตอบสนองต่อการแจ้งเตือน (จาก Flex Message ใน Scheduler)
                handleUpdateReminderStatusAction(params, replyToken);
            }
            default -> log.warn("Unknown action: {}", action);
        }

    }

    private void handleSaveReminderAction(PostbackEvent event, Map<String, String> params, String replyToken) {
        String targetId = params.get("targetId");
        String content = params.get("content");
        String senderId = event.source().userId();

        reminderService.saveReminder(senderId, targetId, content);

        messagingApiClient.replyMessage(new ReplyMessageRequest(
                replyToken, List.of(new TextMessage("✅ บันทึกสำเร็จ!")), false
        ));
    }

    private void handleUpdateReminderStatusAction(Map<String, String> params, String replyToken) {
        UUID reminderId = UUID.fromString(params.get("id"));
        String action = params.get("action");

        // Logic อัปเดตสถานะ (DONE, SNOOZE, CANCEL) ที่เราคุยกันก่อนหน้า
        reminderService.updateStatus(reminderId, replyToken, action);

    }

    // Helper method สำหรับช่วยแกะ String data
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }
}
