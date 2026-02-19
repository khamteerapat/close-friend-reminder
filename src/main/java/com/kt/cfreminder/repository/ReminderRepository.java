package com.kt.cfreminder.repository;

import com.kt.cfreminder.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findByStatusAndRemindAtBefore(String status, LocalDateTime remindAt);
}
