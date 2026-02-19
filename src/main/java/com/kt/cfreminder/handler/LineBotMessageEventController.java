package com.kt.cfreminder.handler;

import com.kt.cfreminder.entity.FollowedUser;
import com.kt.cfreminder.enums.FollowedUserStatus;
import com.kt.cfreminder.enums.LineBotCommand;
import com.kt.cfreminder.repository.FollowedUserRepository;
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
    private final FollowedUserRepository followedUserRepository;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent event) {
        log.info("event: {}", event);

        MessageContent messageContent = event.message();


        if(messageContent instanceof TextMessageContent textContent){

            String originalText = textContent.text();
            String replyToken = event.replyToken();

            if (originalText.startsWith(LineBotCommand.REMIND.getThaiCommand())) {

                List<QuickReplyItem> items = new LinkedList<>();
                //add owner
                String ownerData = String.format("action=save&targetId=%s&content=%s",
                        event.source().userId(), originalText);
                QuickReplyItem ownerReplyItem = new QuickReplyItem(null,
                        new PostbackAction(
                                "ตนเอง", // ชื่อที่แสดงบนปุ่ม (Label)
                                ownerData,          // ข้อมูลที่จะได้รับใน handlePostback
                                "เลือกเตือนคนเอง", // ข้อความที่จะเด้งในแชทเมื่อกด
                                null, null, null
                        )
                );
                items.add(ownerReplyItem);

                List<FollowedUser> allUsers = followedUserRepository.findByStatus(FollowedUserStatus.FOLLOW.name());

                items.addAll(
                        allUsers.stream().map(user -> {
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
                        }).toList()
                );

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

        // ใน SDK 9.x ดึง data ผ่าน event.postback().data()
        String data = event.postback().data();
        String replyToken = event.replyToken();

        if (data != null && data.startsWith("action=save")) {
            // 1. แยกข้อมูลจาก data (Query String format)
            // แนะนำใช้เครื่องมือช่วย Parse หรือตัด String ง่ายๆ:
            Map<String, String> params = parseQueryParams(data);

            String targetId = params.get("targetId"); // userId ของคนที่จะให้เตือน
            String content = params.get("content");   // ข้อความ "จำ จ่ายประกัน..."
            String senderId = event.source().userId(); // userId ของเรา (คนสั่ง)

            // 2. TODO: เขียน Logic สำหรับบันทึกข้อมูลลง Database
            // logic: แยกวันเวลาจาก content -> บันทึก record (sender, target, time, message)
            // reminderService.saveReminder(senderId, targetId, content);

            // 3. ตอบกลับยืนยัน (ฟรี)
            TextMessage response = new TextMessage("บันทึกสำเร็จ! จะเตือนให้ตามเวลาที่ระบุครับ");
            messagingApiClient.replyMessage(new ReplyMessageRequest(
                    replyToken,
                    List.of(response),
                    false
            ));
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
