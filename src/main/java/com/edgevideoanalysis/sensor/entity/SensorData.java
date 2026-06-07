package com.edgevideoanalysis.sensor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sensor_data")
public class SensorData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lampId;

    private Double temperature;

    private Double humidity;

    private Double illumination;

    private Double voltage;

    private Double current;

    private LocalDateTime captureTime;

    private LocalDateTime createTime;
}
