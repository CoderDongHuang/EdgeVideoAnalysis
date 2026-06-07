package com.edgevideoanalysis.alarm.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmRecordVO {

    private Long id;

    private Long lampId;

    private String sensorType;

    private Double sensorValue;

    private Integer alarmLevel;

    private String alarmMessage;

    private LocalDateTime alarmTime;

    private Integer handled;
}
