package com.edgevideoanalysis.control;

import com.edgevideoanalysis.control.dto.ControlCommandDTO;
import com.edgevideoanalysis.control.entity.ControlCommand;
import com.edgevideoanalysis.control.service.IDeviceControlService;
import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DeviceControlServiceTest {

    @Autowired
    private IDeviceControlService deviceControlService;

    @Autowired
    private LampMapper lampMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testControlLED_DeviceOffline() {
        // 使用已存在的灯杆ID（数据库中应该有id=1的记录）
        // 确保设备离线
        stringRedisTemplate.opsForValue().set("device:status:1", "offline");

        ControlCommandDTO dto = new ControlCommandDTO();
        dto.setLampId(1L);
        dto.setCommandType("led_on");

        // 设备离线应该抛出异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            deviceControlService.controlLED(dto);
        });
        assertTrue(exception.getMessage().contains("设备离线"));
    }

    @Test
    public void testControlLED_DeviceNotFound() {
        ControlCommandDTO dto = new ControlCommandDTO();
        dto.setLampId(999999L);
        dto.setCommandType("led_on");

        // 灯杆不存在应该抛出异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            deviceControlService.controlLED(dto);
        });
        assertTrue(exception.getMessage().contains("灯杆不存在"));
    }

    @Test
    public void testGetCommandStatus_NotFound() {
        ControlCommand command = deviceControlService.getCommandStatus(999999L);
        assertNull(command);
    }
}
