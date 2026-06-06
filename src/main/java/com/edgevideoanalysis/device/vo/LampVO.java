package com.edgevideoanalysis.device.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LampVO {

    private Long id;

    private String lampCode;

    private String lampName;

    private String location;

    private String cameraUrl;

    private Integer ledStatus;

    private Integer onlineStatus;

    private LocalDateTime createTime;
}
