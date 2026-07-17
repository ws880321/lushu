package com.roadbook.route.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecordWaypointRequest {
    private String name;
    private Double lng;
    private Double lat;
    private String type = "custom";
    private String note;
    private Integer dayNumber = 1;
}
