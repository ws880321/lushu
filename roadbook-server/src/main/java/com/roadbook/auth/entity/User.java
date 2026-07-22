package com.roadbook.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String openid;

    @Column(length = 64)
    private String unionid;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(nullable = false, length = 16)
    private String role = "user";

    @Column(nullable = false)
    private Integer membership = 0;

    private LocalDateTime memberExpire;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
