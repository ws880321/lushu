package com.roadbook.common;

import com.roadbook.auth.repository.UserRepository;
import com.roadbook.poi.entity.Poi;
import com.roadbook.poi.repository.PoiRepository;
import com.roadbook.route.entity.Route;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.repository.TemplateRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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
    public ApiResponse<Map<String, Object>> stats() {
        long totalUsers = userRepo.count();
        long totalRoutes = routeRepo.count();
        long totalTemplates = templateRepo.count();
        long totalPois = poiRepo.count();

        // Count routes by status
        long publishedRoutes = routeRepo.findAll().stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count();
        long recordingRoutes = routeRepo.findAll().stream().filter(r -> r.getStatus() != null && r.getStatus() == 2).count();

        return ApiResponse.success(Map.of(
                "users", totalUsers,
                "routes", totalRoutes,
                "templates", totalTemplates,
                "pois", totalPois,
                "publishedRoutes", publishedRoutes,
                "recordingRoutes", recordingRoutes
        ));
    }

    @GetMapping("/export/pois")
    public void exportPois(@RequestParam(required = false) String category,
                           @RequestParam(required = false) String province,
                           @RequestParam(required = false) String name,
                           HttpServletResponse resp) throws IOException {
        setCsvHeaders(resp, "pois.csv");
        PrintWriter w = resp.getWriter();
        w.println("ID,名称,类别,省份,城市,地址,自驾评分,停车评分,路况评分,房车友好,可露营,宠物友好");
        List<Poi> pois = poiRepo.findByFilters(category, province, name, PageRequest.of(0, 10000)).getContent();
        for (Poi p : pois) {
            w.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,%d,%d,%d\n",
                    p.getId(), esc(p.getName()), esc(p.getCategory()),
                    esc(p.getProvince()), esc(p.getCity()), esc(p.getAddress()),
                    p.getDriveScore(), p.getParkingScore(), p.getRoadScore(),
                    p.getRvFriendly() != null ? p.getRvFriendly() : 0,
                    p.getCampingAllowed() != null ? p.getCampingAllowed() : 0,
                    p.getPetFriendly() != null ? p.getPetFriendly() : 0);
        }
        w.flush();
    }

    @GetMapping("/export/templates")
    public void exportTemplates(@RequestParam(required = false) String region,
                                HttpServletResponse resp) throws IOException {
        setCsvHeaders(resp, "templates.csv");
        PrintWriter w = resp.getWriter();
        w.println("ID,名称,区域,天数,距离(km),最佳季节,难度,使用次数,状态");
        List<RouteTemplate> templates = region != null && !region.isEmpty()
                ? templateRepo.findByRegionContaining(region, PageRequest.of(0, 10000)).getContent()
                : templateRepo.findAll(PageRequest.of(0, 10000)).getContent();
        for (RouteTemplate t : templates) {
            w.printf("%d,\"%s\",\"%s\",%d,%s,\"%s\",%s,%d,%s\n",
                    t.getId(), esc(t.getName()), esc(t.getRegion()),
                    t.getTotalDays(), t.getTotalDistance(),
                    esc(t.getBestSeason()), t.getDifficulty(),
                    t.getUsageCount(), t.getStatus() == 1 ? "启用" : "禁用");
        }
        w.flush();
    }

    @GetMapping("/export/routes")
    public void exportRoutes(HttpServletResponse resp) throws IOException {
        setCsvHeaders(resp, "routes.csv");
        PrintWriter w = resp.getWriter();
        w.println("ID,标题,天数,距离(km),起点,终点,创建时间,状态");
        List<Route> routes = routeRepo.findAll();
        for (Route r : routes) {
            w.printf("%d,\"%s\",%d,%s,\"%s\",\"%s\",\"%s\",%s\n",
                    r.getId(), esc(r.getTitle()), r.getTotalDays(), r.getTotalDistance(),
                    esc(r.getStartPoint()), esc(r.getEndPoint()),
                    r.getCreatedAt(), r.getStatus() == 1 ? "已发布" : r.getStatus() == 2 ? "记录中" : "草稿");
        }
        w.flush();
    }

    /**
     * List all users (admin only).
     */
    @GetMapping("/users")
    public ApiResponse<?> listUsers() {
        var users = userRepo.findAll().stream().map(u -> Map.of(
            "id", u.getId(), "nickname", u.getNickname(), "phone", u.getPhone() != null ? u.getPhone() : "",
            "role", u.getRole() != null ? u.getRole() : "user",
            "membership", u.getMembership(), "createdAt", u.getCreatedAt()
        )).toList();
        return ApiResponse.success(users);
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = userRepo.findById(id).orElse(null);
        if (user == null) return ApiResponse.error(ErrorCode.NOT_FOUND);
        user.setRole(body.getOrDefault("role", "user"));
        userRepo.save(user);
        return ApiResponse.success(Map.of("ok", true));
    }

    private void setCsvHeaders(HttpServletResponse resp, String filename) {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try { resp.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); } catch (IOException ignored) {}
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
