package com.edgevideoanalysis.sensor.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorQueryDTO {

    private Long lampId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String sensorType;
}
