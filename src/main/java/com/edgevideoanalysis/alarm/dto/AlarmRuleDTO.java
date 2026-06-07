package com.edgevideoanalysis.alarm.dto;

import lombok.Data;

@Data
public class AlarmRuleDTO {

    private Long lampId;

    private String sensorType;

    private Double upperLimit;

    private Double lowerLimit;

    private Integer enabled;
}
