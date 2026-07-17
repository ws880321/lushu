package com.roadbook.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer membership;
}
