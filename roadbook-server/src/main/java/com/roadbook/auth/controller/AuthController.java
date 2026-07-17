package com.roadbook.auth.controller;

import com.roadbook.auth.dto.LoginRequest;
import com.roadbook.auth.dto.LoginResponse;
import com.roadbook.auth.service.WechatAuthService;
import com.roadbook.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final WechatAuthService wechatAuthService;

    public AuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = wechatAuthService.login(
                request.getCode(),
                request.getNickname(),
                request.getAvatarUrl());
        return ApiResponse.success(response);
    }
}
