package com.kt.cfreminder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reminders_table", indexes = {
        @Index(name = "idx_remind_at", columnList = "remind_at"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder extends StandardEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id", nullable = false, length = 50)
    private String senderId;

    @Column(name = "target_id", nullable = false, length = 50)
    private String targetId;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "snooze_count")
    private Integer snoozeCount = 0;
}
