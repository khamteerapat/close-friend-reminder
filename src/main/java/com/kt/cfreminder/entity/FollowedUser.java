package com.kt.cfreminder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "followed_user_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowedUser extends StandardEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "picture_url", columnDefinition = "TEXT")
    private String pictureUrl;

    @Column(length = 20)
    private String status = "FOLLOW";
}
