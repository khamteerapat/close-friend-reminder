package com.kt.cfreminder.handler;

import com.kt.cfreminder.entity.FollowedUser;
import com.kt.cfreminder.enums.FollowedUserStatus;
import com.kt.cfreminder.enums.LineBotCommand;
import com.kt.cfreminder.repository.FollowedUserRepository;
import com.kt.cfreminder.service.ReminderService;
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

import java.util.*;

@Slf4j
@LineMessageHandler
@RequiredArgsConstructor
public class LineBotMessageEventController {
    private final MessagingApiClient messagingApiClient;
    private final ReminderService reminderService;
    private final FollowedUserRepository followedUserRepository;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent event) {
        log.info("event: {}", event);

        MessageContent messageContent = event.message();


        if(messageContent instanceof TextMessageContent textContent){

            String originalText = textContent.text();
            String replyToken = event.replyToken();

            if (originalText.startsWith(LineBotCommand.REMIND.getThaiCommand())) {

                List<FollowedUser> allUsers = followedUserRepository.findByStatus(FollowedUserStatus.FOLLOW.name());

                List<QuickReplyItem> items = allUsers.stream().map(user -> {
                            // สร้าง Data สำหรับส่งกลับมาตอนกดปุ่ม (ต้องไม่เกิน 300 ตัวอักษร)
                            // แนะนำให้ส่ง userId ของคนที่จะถูกเตือนกลับมาด้วย
                            String postbackData = String.format("action=save&targetId=%s&content=%s",
                                    user.getUserId(), originalText);

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
            }
        }


    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        log.info("Postback event: {}", event);
        String data = event.postback().data();
        String replyToken = event.replyToken();

        if (data != null && data.startsWith("action=save")) {
            Map<String, String> params = parseQueryParams(data);
            String targetId = params.get("targetId");
            String content = params.get("content");
            String senderId = event.source().userId();

            try {
                // เรียก Service เพื่อ Parse วันเวลา และบันทึกลง DB (เป็น UTC)
                reminderService.saveReminder(senderId, targetId, content);

                TextMessage response = new TextMessage("✅ บันทึกสำเร็จ! จะเตือนให้ตามเวลาที่ระบุครับ");
                messagingApiClient.replyMessage(new ReplyMessageRequest(replyToken, List.of(response), false));
            } catch (Exception e) {
                log.error("Save reminder failed", e);
                TextMessage error = new TextMessage("❌ บันทึกไม่สำเร็จ: รูปแบบวันเวลาไม่ถูกต้อง (ตัวอย่าง: 23/03/2569 14:45)");
                messagingApiClient.replyMessage(new ReplyMessageRequest(replyToken, List.of(error), false));
            }
        }
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
