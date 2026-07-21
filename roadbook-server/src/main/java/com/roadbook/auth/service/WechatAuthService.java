package com.roadbook.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Service
public class WechatAuthService {

    private final UserRepository userRepository;
    private final SecretKey signingKey;
    private final long expirationMs;
    private final String wechatAppId;
    private final String wechatSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatAuthService(
            UserRepository userRepository,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.wechat.appid}") String wechatAppId,
            @Value("${app.wechat.secret}") String wechatSecret) {
        this.userRepository = userRepository;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.wechatAppId = wechatAppId;
        this.wechatSecret = wechatSecret;
    }

    @Transactional
    public LoginResponse login(String code, String nickname, String avatarUrl) {
        String openid = resolveOpenid(code);

        User user = userRepository.findByOpenid(openid).orElseGet(() -> {
            User newUser = new User();
            newUser.setOpenid(openid);
            newUser.setNickname(nickname != null ? nickname : "微信用户");
            newUser.setAvatarUrl(avatarUrl);
            newUser.setMembership(0);
            return newUser;
        });

        if (nickname != null) user.setNickname(nickname);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        String token = generateToken(user);
        return LoginResponse.builder()
                .token(token).userId(user.getId())
                .nickname(user.getNickname()).avatarUrl(user.getAvatarUrl())
                .membership(user.getMembership()).build();
    }

    private String resolveOpenid(String code) {
        // Admin PC login uses fabricated codes with "admin_" prefix — skip WeChat API
        if (code != null && code.startsWith("admin_")) {
            return "admin_" + java.util.UUID.randomUUID().toString().replace("-", "");
        }
        if (wechatSecret == null || wechatSecret.length() < 20) {
            return "mock_" + java.util.UUID.randomUUID().toString().replace("-", "");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + wechatAppId
                + "&secret=" + wechatSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            if (body.containsKey("openid")) {
                return (String) body.get("openid");
            }
            throw new RuntimeException("微信登录失败: " + body.get("errmsg"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("微信接口调用失败", e);
        }
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("openid", user.getOpenid())
                .issuedAt(now).expiration(expiration)
                .signWith(signingKey).compact();
    }

    public Long parseUserId(String token) {
        String subject = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload().getSubject();
        return Long.parseLong(subject);
    }
}
