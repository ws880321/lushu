package com.roadbook.auth.controller;

import com.roadbook.auth.dto.LoginRequest;
import com.roadbook.auth.dto.LoginResponse;
import com.roadbook.auth.dto.PhoneLoginRequest;
import com.roadbook.auth.dto.SendCodeRequest;
import com.roadbook.auth.service.PhoneAuthService;
import com.roadbook.auth.service.WechatAuthService;
import com.roadbook.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final WechatAuthService wechatAuthService;
    private final PhoneAuthService phoneAuthService;

    public AuthController(WechatAuthService wechatAuthService, PhoneAuthService phoneAuthService) {
        this.wechatAuthService = wechatAuthService;
        this.phoneAuthService = phoneAuthService;
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
