package com.edgevideoanalysis.control.service.impl;

import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;
import com.edgevideoanalysis.control.mapper.ControlCommandMapper;
import com.edgevideoanalysis.control.service.IDeviceControlService;
import com.edgevideoanalysis.control.service.MqttCommandPublisher;
import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 设备控制服务实现
 * HTTP 接收指令 → MQTT 下发到设备 → 设备执行 → 状态记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlServiceImpl implements IDeviceControlService {

    private final ControlCommandMapper controlCommandMapper;
    private final LampMapper lampMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MqttCommandPublisher mqttCommandPublisher;

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

        // 3. 创建控制指令记录（状态：待执行）
        ControlCommand command = new ControlCommand();
        command.setLampId(dto.getLampId());
        command.setCommandType(dto.getCommandType());
        command.setCommandStatus(0); // 0-待执行
        command.setCreateTime(LocalDateTime.now());
        controlCommandMapper.insert(command);

        // 4. 更新状态为执行中
        command.setCommandStatus(1); // 1-执行中
        controlCommandMapper.updateById(command);

        // 5. 通过 MQTT 下发指令到设备
        boolean published = mqttCommandPublisher.publishCommand(
                dto.getLampId(), dto.getCommandType());

        if (published) {
            command.setCommandStatus(2); // 2-成功
            command.setExecuteTime(LocalDateTime.now());
            log.info("LED 控制指令已下发: lampId={}, commandType={}", dto.getLampId(), dto.getCommandType());
        } else {
            command.setCommandStatus(3); // 3-失败
            log.error("LED 控制指令下发失败（MQTT发送失败）: lampId={}", dto.getLampId());
        }
        controlCommandMapper.updateById(command);

        return command;
    }

    @Override
    public ControlCommand getCommandStatus(Long commandId) {
        return controlCommandMapper.selectById(commandId);
    }
}
