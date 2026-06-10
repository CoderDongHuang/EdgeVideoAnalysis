package com.edgevideoanalysis.control.service;

import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;

/**
 * 设备控制服务接口
 */
public interface IDeviceControlService {

    /**
     * 控制设备（如LED开关）
     *
     * @param dto 控制指令
     * @return 控制指令记录
     */
    ControlCommand controlLED(ControlCommandDTO dto);

    /**
     * 查询指令执行状态
     *
     * @param commandId 指令ID
     * @return 指令记录
     */
    ControlCommand getCommandStatus(Long commandId);
}
