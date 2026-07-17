package com.roadbook.auth.service;

import com.roadbook.auth.dto.LoginResponse;
import com.roadbook.auth.entity.User;
import com.roadbook.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class WechatAuthService {

    private final UserRepository userRepository;
    private final SecretKey signingKey;
    private final long expirationMs;

    public WechatAuthService(
            UserRepository userRepository,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.userRepository = userRepository;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    @Transactional
    public LoginResponse login(String code, String nickname, String avatarUrl) {
        // MVP: mock openid — replace with real jscode2session API later
        // TODO: call WeChat jscode2session to get real openid
        String openid = "mock_" + java.util.UUID.randomUUID().toString().replace("-", "");

        // Upsert user
        User user = userRepository.findByOpenid(openid).orElseGet(() -> {
            User newUser = new User();
            newUser.setOpenid(openid);
            newUser.setNickname(nickname != null ? nickname : "微信用户");
            newUser.setAvatarUrl(avatarUrl);
            newUser.setMembership(0);
            // createdAt/updatedAt handled by @PrePersist/@PreUpdate
            return newUser;
        });

        // Update mutable fields on every login
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        String token = generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .membership(user.getMembership())
                .build();
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("openid", user.getOpenid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public Long parseUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }
}
