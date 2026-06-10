package com.edgevideoanalysis.control.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;
import com.edgevideoanalysis.control.service.IDeviceControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备控制控制器
 */
@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
public class DeviceControlController {

    private final IDeviceControlService deviceControlService;

    /**
     * 控制LED开关
     */
    @PostMapping("/led")
    public Result<ControlCommand> controlLED(@RequestBody ControlCommandDTO dto) {
        ControlCommand command = deviceControlService.controlLED(dto);
        return Result.success(command);
    }

    /**
     * 查询指令状态
     */
    @GetMapping("/status/{commandId}")
    public Result<ControlCommand> getCommandStatus(@PathVariable Long commandId) {
        ControlCommand command = deviceControlService.getCommandStatus(commandId);
        if (command == null) {
            return Result.error("指令不存在");
        }
        return Result.success(command);
    }
}
