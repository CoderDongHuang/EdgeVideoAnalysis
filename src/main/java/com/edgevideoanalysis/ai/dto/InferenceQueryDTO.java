package com.edgevideoanalysis.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InferenceQueryDTO {

    private Long lampId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
