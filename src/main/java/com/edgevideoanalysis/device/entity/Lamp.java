package com.edgevideoanalysis.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_lamp")
public class Lamp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String lampCode;

    private String lampName;

    private String location;

    private String cameraUrl;

    private Integer ledStatus;

    private Integer onlineStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
