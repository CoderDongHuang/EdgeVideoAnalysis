package com.edgevideoanalysis.control.service.impl;

import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;
import com.edgevideoanalysis.control.mapper.ControlCommandMapper;
import com.edgevideoanalysis.control.service.IDeviceControlService;
import com.edgevideoanalysis.device.mapper.LampMapper;
import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.websocket.handler.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 设备控制服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlServiceImpl implements IDeviceControlService {

    private final ControlCommandMapper controlCommandMapper;
    private final LampMapper lampMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEVICE_STATUS_KEY = "device:status:";

    @Override
    public ControlCommand controlLED(ControlCommandDTO dto) {
        // 1. 验证设备是否存在
        Lamp lamp = lampMapper.selectById(dto.getLampId());
        if (lamp == null) {
            throw new RuntimeException("灯杆不存在: lampId=" + dto.getLampId());
        }

        // 2. 验证设备在线状态
        String statusKey = DEVICE_STATUS_KEY + dto.getLampId();
        String status = stringRedisTemplate.opsForValue().get(statusKey);
        if (!"online".equals(status)) {
            throw new RuntimeException("设备离线: lampId=" + dto.getLampId());
        }

        // 3. 创建控制指令记录
        ControlCommand command = new ControlCommand();
        command.setLampId(dto.getLampId());
        command.setCommandType(dto.getCommandType());
        command.setCommandStatus("pending");
        command.setCreateTime(LocalDateTime.now());
        controlCommandMapper.insert(command);

        // 4. 下发控制命令（通过WebSocket推送）
        try {
            // 更新状态为执行中
            command.setCommandStatus("executing");
            controlCommandMapper.updateById(command);

            // TODO: 实际项目中通过WebSocket或HTTP下发指令到设备
            // 这里模拟指令执行成功
            command.setCommandStatus("success");
            command.setExecuteTime(LocalDateTime.now());
            controlCommandMapper.updateById(command);

            log.info("LED控制指令下发成功: lampId={}, commandType={}", dto.getLampId(), dto.getCommandType());
        } catch (Exception e) {
            command.setCommandStatus("failed");
            controlCommandMapper.updateById(command);
            log.error("LED控制指令下发失败: lampId={}", dto.getLampId(), e);
        }

        return command;
    }

    @Override
    public ControlCommand getCommandStatus(Long commandId) {
        return controlCommandMapper.selectById(commandId);
    }
}
