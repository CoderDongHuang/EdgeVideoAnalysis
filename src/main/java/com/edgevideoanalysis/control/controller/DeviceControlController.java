package com.edgevideoanalysis.control.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;
import com.edgevideoanalysis.control.service.IDeviceControlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "远程控制")
@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
public class DeviceControlController {

    private final IDeviceControlService deviceControlService;

    @ApiOperation("下发 LED 控制指令（led_on / led_off）")
    @PostMapping("/led")
    public Result<ControlCommand> controlLED(@RequestBody ControlCommandDTO dto) {
        ControlCommand command = deviceControlService.controlLED(dto);
        return Result.success(command);
    }

    @ApiOperation("查询控制指令执行状态")
    @GetMapping("/status/{commandId}")
    public Result<ControlCommand> getCommandStatus(
            @ApiParam("指令ID") @PathVariable Long commandId) {
        ControlCommand command = deviceControlService.getCommandStatus(commandId);
        if (command == null) {
            return Result.error("指令不存在");
        }
        return Result.success(command);
    }
}
