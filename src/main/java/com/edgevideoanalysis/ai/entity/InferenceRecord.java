package com.edgevideoanalysis.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inference_record")
public class InferenceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lampId;

    private String originalImage;

    private String processedImage;

    private Integer personCount;

    private String inferenceResults;

    private Long inferenceTime;

    private LocalDateTime createTime;
}
