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
     * 指令状态: 0-待执行 1-执行中 2-成功 3-失败
     */
    private Integer commandStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;
}
