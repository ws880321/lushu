package com.roadbook.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoCodeResponse {

    private String status;
    private String info;
    private List<GeoCode> geocodes;
    private RegeoCode regeocode;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeoCode {
        @JsonProperty("formatted_address")
        private String formattedAddress;
        private String country;
        private String province;
        private String city;
        private String district;
        private String township;
        private String location;
        private String level;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RegeoCode {
        @JsonProperty("formatted_address")
        private String formattedAddress;
        private AddressComponent addressComponent;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressComponent {
        private String country;
        private String province;
        private String city;
        private String district;
        private String adcode;
        @JsonProperty("towncode")
        private String townCode;
        private String town;
    }
}
