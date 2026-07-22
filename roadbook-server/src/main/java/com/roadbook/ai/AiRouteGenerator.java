package com.roadbook.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiRouteGenerator {

    @Value("${app.deepseek.key}") private String apiKey;
    @Value("${app.deepseek.url}") private String apiUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public record AiWaypoint(int day, String type, String name, String tips, int stayMin) {}
    public record AiRoute(String title, String description, int totalDays, int totalDistanceKm,
                          List<AiWaypoint> waypoints) {}

    public AiRoute generate(String start, String end, int days, List<String> tags,
                            String difficulty, double dailyHours) {
        String prompt = buildPrompt(start, end, days, tags, difficulty, dailyHours);
        try {
            Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                    Map.of("role", "system", "content", "你是中国自驾游专家。只返回纯净JSON，不要任何解释。"),
                    Map.of("role", "user", "content", prompt)),
                "temperature", 0.7, "max_tokens", 4096);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return parse(resp.body());
        } catch (Exception e) {
            log.error("AI generation failed", e);
            return null;
        }
    }

    private String buildPrompt(String start, String end, int days, List<String> tags,
                                String diff, double hours) {
        return String.format("""
            为中国自驾游生成路线规划JSON（waypoints必须是单个数组包含所有天的途经点）：

            起点：%s | 终点：%s | 天数：%d | 偏好：%s | 难度：%s | 日驾驶≤%.0fh

            {"title":"标题","description":"2-3句描述","totalDays":%d,"totalDistanceKm":公里数,
             "waypoints":[{"day":1,"type":"scenic|food|hotel|gas|photo|custom","name":"地点名",
             "tips":"停车/路况/门票建议","stayMin":停留分钟}]}

            要求：每天4-8个途经点、地点真实可查、总里程合理、tips实用。
            waypoints是单个JSON数组，按day字段区分每天，不要分成多个数组。
            只返回JSON，不要加```标记或解释文字。""",
            start, end, days, tags != null ? String.join(",", tags) : "", diff, hours, days);
    }

    private AiRoute parse(String raw) {
        try {
            DeepSeekResp r = mapper.readValue(raw, DeepSeekResp.class);
            String content = r.getChoices().get(0).getMessage().getContent().trim();
            // Strip markdown fences if present
            if (content.startsWith("```")) {
                content = content.substring(content.indexOf("\n") + 1);
                content = content.substring(0, content.lastIndexOf("```")).trim();
                if (content.startsWith("json")) content = content.substring(4).trim();
            }
            return mapper.readValue(content, AiRoute.class);
        } catch (Exception e) {
            log.error("Parse AI response failed: {}", raw, e);
            return null;
        }
    }

    /**
     * Adjust an existing route based on a user's conversational request.
     */
    public AiRoute adjust(String routeJson, String userMessage) {
        String prompt = String.format("""
            你是中国自驾游专家。以下是一条已有的路线规划（JSON格式）：

            %s

            用户要求：%s

            请根据用户要求修改路线，返回修改后的完整JSON。
            要求：保持合理的总天数、距离和途经点数量。地点必须真实可查。
            只返回JSON，不要加```标记或解释文字。
            """, routeJson, userMessage);

        try {
            Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                    Map.of("role", "system", "content", "你是中国自驾游专家。只返回纯净JSON，不要任何解释。"),
                    Map.of("role", "user", "content", prompt)),
                "temperature", 0.7, "max_tokens", 4096);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return parse(resp.body());
        } catch (Exception e) {
            log.error("AI adjustment failed", e);
            return null;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class DeepSeekResp { private List<Choice> choices;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice { private Message message;
            @Data @JsonIgnoreProperties(ignoreUnknown = true)
            static class Message { private String content; }
        }
    }
}
