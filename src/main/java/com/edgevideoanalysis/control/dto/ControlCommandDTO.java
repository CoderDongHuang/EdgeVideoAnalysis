package com.edgevideoanalysis.control.dto;

import lombok.Data;

/**
 * 设备控制指令DTO
 */
@Data
public class ControlCommandDTO {

    /**
     * 灯杆ID
     */
    private Long lampId;

    /**
     * 指令类型: led_on / led_off
     */
    private String commandType;
}
