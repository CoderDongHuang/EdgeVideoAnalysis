package com.edgevideoanalysis.ai.vo;

import lombok.Data;

@Data
public class InferenceResultVO {

    private Long lampId;

    private String originalImage;

    private String processedImage;

    private Integer personCount;

    private String inferenceResults;

    private Long inferenceTime;
}
