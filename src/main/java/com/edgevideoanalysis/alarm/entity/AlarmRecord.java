package com.edgevideoanalysis.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alarm_record")
public class AlarmRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lampId;

    private String sensorType;

    private Double sensorValue;

    private Integer alarmLevel;

    private String alarmMessage;

    private LocalDateTime alarmTime;

    private Integer handled;

    private LocalDateTime createTime;
}
