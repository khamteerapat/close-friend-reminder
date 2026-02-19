package com.kt.cfreminder.handler;

import com.kt.cfreminder.entity.FollowedUser;
import com.kt.cfreminder.enums.FollowedUserStatus;
import com.kt.cfreminder.repository.FollowedUserRepository;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.UserProfileResponse;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.FollowEvent;
import com.linecorp.bot.webhook.model.UnfollowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@LineMessageHandler
@RequiredArgsConstructor
public class LineBotFollowEventController {
    private static final String SYSTEM = "SYSTEM";

    private final MessagingApiClient messagingApiClient;
    private final FollowedUserRepository followedUserRepository;

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String userId = event.source().userId();

        try {
            // 2. ใช้ MessagingApiClient ไปดึงโปรไฟล์จาก LINE (เพื่อเอา Display Name)
            // ตัวนี้จะยิง API ไปที่ https://api.line.me/v2/bot/profile/{userId}
            UserProfileResponse profile = messagingApiClient.getProfile(userId).get().body();

            String displayName = profile.displayName();
            String pictureUrl = profile.pictureUrl() != null ? profile.pictureUrl().toString() : null;

            // 3. บันทึกลง Database
            // แนะนำให้ใช้ logic "Upsert" (ถ้ามีอยู่แล้วให้ Update ถ้ายังไม่มีให้ Insert)
            FollowedUser user = followedUserRepository.findByUserId(userId)
                    .orElse(new FollowedUser());

            user.setUserId(userId);
            user.setDisplayName(displayName);
            user.setPictureUrl(pictureUrl);
            user.setCreatedBy(SYSTEM);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            user.setStatus(FollowedUserStatus.FOLLOW.name()); // เก็บสถานะไว้เผื่อเขา Block ในอนาคต

            followedUserRepository.save(user);

            log.info("เพิ่มผู้ใช้ใหม่เรียบร้อย: {}", displayName);

        } catch (Exception e) {
            log.error("ไม่สามารถดึงข้อมูลโปรไฟล์สำหรับ userId: {}", userId, e);
        }
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        String userId = event.source().userId();

        // ปรับสถานะใน DB ให้เป็น Blocked หรือจะลบออกก็ได้
        followedUserRepository.findByUserId(userId).ifPresent(user -> {
            user.setStatus(FollowedUserStatus.UNFOLLOW.name());
            user.setUpdatedBy(SYSTEM);
            user.setUpdatedAt(Instant.now());
            followedUserRepository.save(user);
        });

        log.info("ผู้ใช้ Block บอทแล้ว: {}", userId);
    }
}
