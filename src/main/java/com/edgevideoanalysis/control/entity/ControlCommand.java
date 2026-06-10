package com.edgevideoanalysis.control.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备控制指令实体类
 */
@Data
@TableName("t_control_command")
public class ControlCommand {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 灯杆ID
     */
    private Long lampId;

    /**
     * 指令类型: led_on / led_off
     */
    private String commandType;

    /**
     * 指令状态: pending / executing / success / failed
     */
    private String commandStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;
}
