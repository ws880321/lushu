package com.roadbook.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private String status;
    private String info;
    private String count;
    private List<LiveWeather> lives;
    private List<Forecast> forecasts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LiveWeather {
        private String province;
        private String city;
        private String adcode;
        private String weather;
        private String temperature;
        private String winddirection;
        private String windpower;
        private String humidity;
        @JsonProperty("report_time")
        private String reportTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Forecast {
        private String province;
        private String city;
        private String adcode;
        @JsonProperty("report_time")
        private String reportTime;
        private List<ForecastCast> casts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastCast {
        private String date;
        private String week;
        @JsonProperty("dayweather")
        private String dayWeather;
        @JsonProperty("nightweather")
        private String nightWeather;
        @JsonProperty("daytemp")
        private String dayTemp;
        @JsonProperty("nighttemp")
        private String nightTemp;
        @JsonProperty("daywind")
        private String dayWind;
        @JsonProperty("nightwind")
        private String nightWind;
    }
}
