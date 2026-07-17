package com.roadbook.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrivingRouteResponse {

    private String status;
    private String info;
    private Route route;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private String origin;
        private String destination;
        private List<Path> paths;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        private String distance;
        private String duration;
        private String strategy;
        private List<Step> steps;
        private List<Road> roads;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        private String instruction;
        private String road;
        private String distance;
        private String duration;
        private String polyline;
        private String action;
        @JsonProperty("assistant_action")
        private String assistantAction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Road {
        private String name;
        private String distance;
        private String duration;
        private String direction;
    }
}
