package com.edgevideoanalysis.sensor.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDataDTO {

    private Long lampId;

    private Double temperature;

    private Double humidity;

    private Double illumination;

    private Double voltage;

    private Double current;

    private LocalDateTime captureTime;
}
