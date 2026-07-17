package com.roadbook.amap;

import com.roadbook.amap.config.AmapConfig;
import com.roadbook.amap.dto.DrivingRouteResponse;
import com.roadbook.amap.dto.GeoCodeResponse;
import com.roadbook.amap.dto.PoiSearchResponse;
import com.roadbook.amap.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmapClient {

    private final AmapConfig config;

    private final WebClient webClient = WebClient.create("https://restapi.amap.com");

    public GeoCodeResponse geocode(String address) {
        String url = UriComponentsBuilder.fromPath("/v3/geocode/geo")
                .queryParam("key", config.getKey())
                .queryParam("address", address)
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(GeoCodeResponse.class).block();
    }

    public GeoCodeResponse regeocode(double lng, double lat) {
        String location = lng + "," + lat;
        String url = UriComponentsBuilder.fromPath("/v3/geocode/regeo")
                .queryParam("key", config.getKey())
                .queryParam("location", location)
                .queryParam("extensions", "base")
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(GeoCodeResponse.class).block();
    }

    public PoiSearchResponse searchPoi(String keywords, String types, String city, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v3/place/text")
                .queryParam("key", config.getKey())
                .queryParam("keywords", keywords)
                .queryParam("city", city != null ? city : "全国")
                .queryParam("offset", 20)
                .queryParam("page", page)
                .queryParam("extensions", "all");
        if (types != null) {
            builder.queryParam("types", types);
        }
        return webClient.get().uri(builder.build().toUriString()).retrieve()
                .bodyToMono(PoiSearchResponse.class).block();
    }

    public DrivingRouteResponse drivingRoute(String origin, String destination, String waypoints) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v3/direction/driving")
                .queryParam("key", config.getKey())
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("strategy", "10")
                .queryParam("extensions", "all");
        if (waypoints != null && !waypoints.isEmpty()) {
            builder.queryParam("waypoints", waypoints);
        }
        return webClient.get().uri(builder.build().toUriString()).retrieve()
                .bodyToMono(DrivingRouteResponse.class).block();
    }

    public WeatherResponse weather(String city) {
        String url = UriComponentsBuilder.fromPath("/v3/weather/weatherInfo")
                .queryParam("key", config.getKey())
                .queryParam("city", city)
                .queryParam("extensions", "all")
                .build().toUriString();
        return webClient.get().uri(url).retrieve()
                .bodyToMono(WeatherResponse.class).block();
    }
}
