package com.edgevideoanalysis.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alarm_rule")
public class AlarmRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lampId;

    private String sensorType;

    private Double upperLimit;

    private Double lowerLimit;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
