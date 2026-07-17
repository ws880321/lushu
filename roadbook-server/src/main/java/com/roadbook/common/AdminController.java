package com.roadbook.common;

import com.roadbook.auth.repository.UserRepository;
import com.roadbook.poi.repository.PoiRepository;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final RouteRepository routeRepo;
    private final TemplateRepository templateRepo;
    private final PoiRepository poiRepo;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(Map.of(
                "users", userRepo.count(),
                "routes", routeRepo.count(),
                "templates", templateRepo.count(),
                "pois", poiRepo.count()
        ));
    }
}
