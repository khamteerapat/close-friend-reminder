package com.kt.cfreminder.repository;

import com.kt.cfreminder.entity.FollowedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowedUserRepository extends JpaRepository<FollowedUser, UUID> {
    Optional<FollowedUser> findByUserId(String userId);
    List<FollowedUser> findByStatus(String status);
}
