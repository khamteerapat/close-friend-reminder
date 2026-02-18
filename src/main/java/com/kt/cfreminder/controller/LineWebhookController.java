package com.kt.cfreminder.controller;

import com.linecorp.bot.model.event.CallbackRequest;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/line")
public class LineWebhookController {
    @PostMapping
    public void callback(@RequestBody CallbackRequest request) {
        // วนลูปเช็คทุก Event ที่ LINE ส่งมา (เผื่อมาพร้อมกันหลายอัน)
        request.getEvents().forEach(event -> {

            // ตรวจสอบว่าเป็น Message Event หรือไม่
            if (event instanceof MessageEvent<?> messageEvent) {

                // ตรวจสอบว่าเป็นข้อความตัวอักษร (Text) หรือไม่
                if (messageEvent.getMessage() instanceof TextMessageContent textMessage) {
                    String userText = textMessage.getText();
                    String userId = event.getSource().getUserId();

                    System.out.println("User " + userId + " ส่งมาว่า: " + userText);
                }
            }
            // สามารถเช็ค Event อื่นๆ เช่น FollowEvent, JoinEvent ได้ที่นี่
        });
    }
}
