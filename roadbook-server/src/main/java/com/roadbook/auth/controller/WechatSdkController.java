package com.roadbook.auth.controller;

import com.roadbook.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class WechatSdkController {

    @Value("${app.wechat.appid}")
    private String appId;

    @GetMapping("/js-sdk-sign")
    public ApiResponse<Map<String, Object>> sign(@RequestParam String url, @RequestAttribute(value = "userId", required = false) Long userId) {
        try {
            String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            long timestamp = System.currentTimeMillis() / 1000;
            String ticket = getJsApiTicket();
            String str = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr + "&timestamp=" + timestamp + "&url=" + url;
            String signature = sha1(str);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("appId", appId); data.put("timestamp", timestamp);
            data.put("nonceStr", nonceStr); data.put("signature", signature);
            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(com.roadbook.common.ErrorCode.INTERNAL_ERROR);
        }
    }

    // Mock: in production, fetch from WeChat API using access_token
    private String getJsApiTicket() {
        return "mock_ticket_for_dev";
    }

    private String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
