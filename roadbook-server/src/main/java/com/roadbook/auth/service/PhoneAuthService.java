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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhoneAuthService {

    private final UserRepository userRepository;
    private final SecretKey signingKey;
    private final long expirationMs;

    // In-memory code store: phone -> code (use Redis in production)
    private final ConcurrentHashMap<String, String> codeStore = new ConcurrentHashMap<>();

    public PhoneAuthService(
            UserRepository userRepository,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.userRepository = userRepository;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public Map<String, Object> sendCode(String phone) {
        // Mock mode: always use "000000" as code
        String code = "000000";
        codeStore.put(phone, code);
        return Map.of("phone", phone, "mock", true);
    }

    @Transactional
    public LoginResponse login(String phone, String code, String nickname) {
        // Verify code (mock mode: "000000" always accepted)
        String storedCode = codeStore.get(phone);
        if (storedCode == null && !"000000".equals(code)) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!"000000".equals(code) && !code.equals(storedCode)) {
            throw new RuntimeException("验证码错误");
        }
        codeStore.remove(phone);

        String openid = "phone_" + phone;

        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User newUser = new User();
            newUser.setOpenid(openid);
            newUser.setPhone(phone);
            newUser.setNickname(nickname != null ? nickname : "自驾用户");
            newUser.setMembership(0);
            return newUser;
        });

        if (nickname != null) user.setNickname(nickname);
        user.setUpdatedAt(LocalDateTime.now());
        user.setOpenid(openid); // keep openid in sync
        user.setPhone(phone);
        user = userRepository.save(user);

        String token = generateToken(user);
        return LoginResponse.builder()
                .token(token).userId(user.getId())
                .nickname(user.getNickname()).avatarUrl(user.getAvatarUrl())
                .membership(user.getMembership()).build();
    }

    private String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("openid", user.getOpenid())
                .issuedAt(now).expiration(expiration)
                .signWith(signingKey).compact();
    }
}
