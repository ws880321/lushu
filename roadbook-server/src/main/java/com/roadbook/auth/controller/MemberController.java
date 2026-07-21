package com.roadbook.auth.controller;

import com.roadbook.auth.entity.User;
import com.roadbook.auth.repository.UserRepository;
import com.roadbook.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class MemberController {
    private final UserRepository userRepo;

    public MemberController(UserRepository userRepo) { this.userRepo = userRepo; }

    @GetMapping("/member/plans")
    public ApiResponse<List<Map<String, Object>>> plans() {
        List<Map<String, Object>> plans = new ArrayList<>();
        plans.add(Map.of("id", 0, "name", "免费版", "price", 0, "dailyGenerates", 3, "features", "每日生成3条路书|基础路线规划|社区浏览"));
        plans.add(Map.of("id", 1, "name", "Pro版", "price", 19.9, "dailyGenerates", 99, "features", "无限生成路书|天气预报|优先路线规划|PDF导出"));
        plans.add(Map.of("id", 2, "name", "终身版", "price", 99, "dailyGenerates", 999, "features", "全部功能|永久有效|专属客服"));
        return ApiResponse.success(plans);
    }

    @PostMapping("/member/upgrade")
    public ApiResponse<Map<String, Object>> upgrade(@RequestAttribute("userId") Long userId, @RequestBody Map<String, Integer> body) {
        int planId = body.getOrDefault("planId", 0);
        User user = userRepo.findById(userId).orElseThrow();
        user.setMembership(planId);
        if (planId > 0) user.setMemberExpire(LocalDateTime.now().plusDays(planId == 2 ? 36500 : 30));
        userRepo.save(user);
        return ApiResponse.success(Map.of("membership", user.getMembership(), "expire", user.getMemberExpire() + ""));
    }

    @GetMapping("/member/usage")
    public ApiResponse<Map<String, Object>> usage(@RequestAttribute("userId") Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return ApiResponse.success(Map.of("membership", user.getMembership(), "expire", user.getMemberExpire() + ""));
    }
}
