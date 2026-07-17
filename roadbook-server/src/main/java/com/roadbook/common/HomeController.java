package com.roadbook.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.success(Map.of(
                "app", "路书 API",
                "version", "0.1.0",
                "time", LocalDateTime.now().toString()
        ));
    }
}
