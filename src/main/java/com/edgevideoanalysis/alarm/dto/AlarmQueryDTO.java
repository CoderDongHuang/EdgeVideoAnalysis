package com.edgevideoanalysis.alarm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmQueryDTO {

    private Long lampId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer handled;
}
