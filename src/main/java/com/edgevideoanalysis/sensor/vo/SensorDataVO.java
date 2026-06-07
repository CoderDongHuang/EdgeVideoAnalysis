package com.edgevideoanalysis.sensor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDataVO {

    private Long id;

    private Long lampId;

    private Double temperature;

    private Double humidity;

    private Double illumination;

    private Double voltage;

    private Double current;

    private LocalDateTime captureTime;
}
