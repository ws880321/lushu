package com.roadbook.auth.controller;

import com.roadbook.auth.dto.LoginRequest;
import com.roadbook.auth.dto.LoginResponse;
import com.roadbook.auth.dto.PhoneLoginRequest;
import com.roadbook.auth.dto.SendCodeRequest;
import com.roadbook.auth.entity.User;
import com.roadbook.auth.repository.UserRepository;
import com.roadbook.auth.service.PhoneAuthService;
import com.roadbook.auth.service.WechatAuthService;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final WechatAuthService wechatAuthService;
    private final PhoneAuthService phoneAuthService;
    private final UserRepository userRepo;

    public AuthController(WechatAuthService wechatAuthService, PhoneAuthService phoneAuthService,
                         UserRepository userRepo) {
        this.wechatAuthService = wechatAuthService;
        this.phoneAuthService = phoneAuthService;
        this.userRepo = userRepo;
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                userId = wechatAuthService.parseUserId(auth.substring(7));
            } catch (Exception ignored) {}
        }
        if (userId == null) return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        return ApiResponse.success(Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "role", user.getRole() != null ? user.getRole() : "user",
            "membership", user.getMembership(),
            "createdAt", user.getCreatedAt()
        ));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = wechatAuthService.login(
                request.getCode(),
                request.getNickname(),
                request.getAvatarUrl());
        return ApiResponse.success(response);
    }

    @PostMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        Map<String, Object> result = phoneAuthService.sendCode(request.getPhone());
        return ApiResponse.success(result);
    }

    @PostMapping("/phone-login")
    public ApiResponse<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        LoginResponse response = phoneAuthService.login(
                request.getPhone(),
                request.getCode(),
                request.getNickname());
        return ApiResponse.success(response);
    }
}
