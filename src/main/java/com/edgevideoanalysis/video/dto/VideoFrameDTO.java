package com.edgevideoanalysis.video.dto;

import lombok.Data;

@Data
public class VideoFrameDTO {

    private Long lampId;

    private String frameData;

    private Long timestamp;
}
