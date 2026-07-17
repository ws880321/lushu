package com.roadbook.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoiSearchResponse {

    private String status;
    private String info;
    private int count;
    private List<Poi> pois;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Poi {
        private String id;
        private String name;
        private String type;
        private String typecode;
        private String address;
        private String location;
        private String tel;
        private String distance;
        @JsonProperty("business_area")
        private String businessArea;
    }
}
