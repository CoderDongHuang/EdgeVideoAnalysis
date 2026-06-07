package com.edgevideoanalysis.sensor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorCurveVO {

    private LocalDateTime time;

    private Double value;
}
