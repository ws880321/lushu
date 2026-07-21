package com.roadbook.share.controller;

import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Server-side rendered HTML share page with dynamic OG meta tags.
 * Crawlers (WeChat, Twitter, Facebook) get full OG tags.
 * Human visitors are redirected to the SPA.
 */
@Controller
public class SharePageController {

    private final RouteRepository routeRepo;
    private final RouteWaypointRepository waypointRepo;

    public SharePageController(RouteRepository routeRepo, RouteWaypointRepository waypointRepo) {
        this.routeRepo = routeRepo;
        this.waypointRepo = waypointRepo;
    }

    @GetMapping(value = "/share/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String sharePage(@PathVariable Long id) {
        Route route = routeRepo.findById(id).orElse(null);
        if (route == null || route.getIsPublic() == null || route.getIsPublic() != 1) {
            return notFoundPage(id);
        }

        List<RouteWaypoint> waypoints = waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(id);

        String title = esc(route.getTitle());
        String description = buildDescription(route, waypoints);
        String image = findThumbnail(route, waypoints);
        String url = "https://www.wychen.net/share/" + id;
        String days = String.valueOf(route.getTotalDays() != null ? route.getTotalDays() : 0);
        String distance = String.valueOf(route.getTotalDistance() != null ? route.getTotalDistance() : 0);
        String start = esc(route.getStartPoint() != null ? route.getStartPoint() : "");
        String end = esc(route.getEndPoint() != null ? route.getEndPoint() : "");

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
<title>%s - 路书</title>
<meta name="description" content="%s">
<meta property="og:title" content="%s">
<meta property="og:description" content="%s">
<meta property="og:image" content="%s">
<meta property="og:url" content="%s">
<meta property="og:type" content="article">
<meta property="og:site_name" content="路书">
<meta property="og:locale" content="zh_CN">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="%s">
<meta name="twitter:description" content="%s">
<meta name="twitter:image" content="%s">
<meta itemprop="name" content="%s">
<meta itemprop="description" content="%s">
<meta itemprop="image" content="%s">
<meta http-equiv="refresh" content="0;url=/app/#/share/%d">
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;background:#f5f5f5;min-height:100vh;display:flex;align-items:center;justify-content:center}
.card{max-width:400px;width:90%%;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 2px 20px rgba(0,0,0,.08)}
.hero{width:100%%;height:200px;object-fit:cover;background:linear-gradient(135deg,#2E7D32,#1B5E20)}
.body{padding:20px}
h1{font-size:20px;color:#333;margin-bottom:8px}
.desc{font-size:14px;color:#666;line-height:1.6;margin-bottom:12px}
.stats{display:flex;gap:16px;font-size:13px;color:#2E7D32;margin-bottom:16px;flex-wrap:wrap}
.stats span{background:#e8f5e9;padding:4px 12px;border-radius:12px}
.btn{display:block;text-align:center;background:#2E7D32;color:#fff;padding:12px 0;border-radius:10px;font-size:15px;text-decoration:none;font-weight:600}
.footer{text-align:center;color:#aaa;font-size:12px;padding:16px}
</style>
</head>
<body>
<div class="card">
<img class="hero" src="%s" alt="%s" onerror="this.style.display='none'">
<div class="body">
<h1>%s</h1>
<p class="desc">%s</p>
<div class="stats"><span>%s天</span><span>%skm</span><span>%s → %s</span></div>
<a class="btn" href="/app/#/share/%d">在路书 App 中查看 →</a>
</div>
</div>
<div class="footer">由 路书 生成 · www.wychen.net</div>
<script>if(!/bot|crawler|spider|facebookexternalhit|twitterbot|whatsapp|slack|telegram|wechat|baidu|google|bing/i.test(navigator.userAgent||'')){setTimeout(function(){window.location.href='/app/#/share/%d'},600)}</script>
</body>
</html>
""".formatted(
            title, description,
            title, description, image, url,
            title, description, image,
            title, description, image,
            id,
            image, title, title, description,
            days, distance, start, end,
            id,
            id
        );
    }

    private String notFoundPage(Long id) {
        return """
<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<title>路书 - 路线不存在</title>
<meta property="og:title" content="路书 - 路线不存在">
<meta property="og:description" content="该路线不存在或未公开">
<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;background:#f5f5f5;margin:0}.msg{text-align:center}.msg h1{color:#999;font-size:48px;margin:0}.msg p{color:#666;margin:8px 0 24px}.msg a{color:#2E7D32;text-decoration:none;font-size:16px}</style>
</head><body><div class="msg"><h1>404</h1><p>路线不存在或未公开</p><a href="/app/">打开路书 App</a></div></body></html>
""";
    }

    private String buildDescription(Route route, List<RouteWaypoint> wps) {
        if (route.getDescription() != null && !route.getDescription().isBlank()) {
            return esc(route.getDescription().substring(0, Math.min(200, route.getDescription().length())));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(esc(route.getTitle())).append(" · ");
        sb.append(route.getTotalDays()).append("天");
        if (route.getTotalDistance() != null) sb.append(route.getTotalDistance()).append("km");
        if (route.getStartPoint() != null) sb.append(" · ").append(route.getStartPoint());
        if (route.getEndPoint() != null) sb.append(" → ").append(route.getEndPoint());
        if (wps != null && !wps.isEmpty() && sb.length() < 150) {
            sb.append(" · 途经：");
            for (int i = 0; i < Math.min(5, wps.size()); i++) {
                if (i > 0) sb.append("、");
                sb.append(wps.get(i).getName());
                if (sb.length() > 200) break;
            }
        }
        return sb.toString();
    }

    private String findThumbnail(Route route, List<RouteWaypoint> wps) {
        if (route.getThumbnailUrl() != null && !route.getThumbnailUrl().isBlank()) {
            String t = route.getThumbnailUrl();
            return t.startsWith("http") ? t : "https://www.wychen.net" + (t.startsWith("/") ? "" : "/") + t;
        }
        if (wps != null) {
            for (RouteWaypoint wp : wps) {
                if (wp.getPhotoUrl() != null && !wp.getPhotoUrl().isBlank()) {
                    String p = wp.getPhotoUrl();
                    return p.startsWith("http") ? p : "https://www.wychen.net" + (p.startsWith("/") ? "" : "/") + p;
                }
            }
        }
        return "https://www.wychen.net/app/og-image.png";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
