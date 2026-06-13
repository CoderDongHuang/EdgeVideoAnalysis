package com.edgevideoanalysis.alarm;

import com.edgevideoanalysis.alarm.entity.AlarmRecord;
import com.edgevideoanalysis.alarm.entity.AlarmRule;
import com.edgevideoanalysis.alarm.mapper.AlarmRecordMapper;
import com.edgevideoanalysis.alarm.service.IAlarmRuleService;
import com.edgevideoanalysis.alarm.service.impl.AlarmRecordServiceImpl;
import com.edgevideoanalysis.sensor.entity.SensorData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AlarmRecordServiceImpl 单元测试
 * 覆盖阈值告警判定的核心逻辑：超上限 / 低下限 / 正常 / 无规则 / 空值
 */
@ExtendWith(MockitoExtension.class)
class AlarmRecordServiceImplTest {

    @Mock
    private AlarmRecordMapper alarmRecordMapper;

    @Mock
    private IAlarmRuleService alarmRuleService;

    @InjectMocks
    private AlarmRecordServiceImpl alarmRecordService;

    // ==================== 超上限告警 ====================

    @Test
    @DisplayName("温度超上限 → 触发告警")
    void testTemperatureExceedsUpperLimit() {
        SensorData sensorData = buildSensorData(1L, 45.0, null, null, null, null);

        AlarmRule rule = new AlarmRule();
        rule.setUpperLimit(40.0);
        rule.setLowerLimit(0.0);
        when(alarmRuleService.getRule(1L, "temperature")).thenReturn(rule);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordMapper).insert(captor.capture());

        AlarmRecord record = captor.getValue();
        assertEquals(1L, record.getLampId());
        assertEquals("temperature", record.getSensorType());
        assertEquals(45.0, record.getSensorValue());
        assertTrue(record.getAlarmMessage().contains("超过上限"));
        assertEquals(1, record.getAlarmLevel());
    }

    // ==================== 低下限告警 ====================

    @Test
    @DisplayName("电压低于下限 → 触发告警")
    void testVoltageBelowLowerLimit() {
        SensorData sensorData = buildSensorData(1L, null, null, null, 2.5, null);

        AlarmRule rule = new AlarmRule();
        rule.setLowerLimit(3.0);
        when(alarmRuleService.getRule(1L, "voltage")).thenReturn(rule);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordMapper).insert(captor.capture());

        AlarmRecord record = captor.getValue();
        assertEquals("voltage", record.getSensorType());
        assertEquals(2.5, record.getSensorValue());
        assertTrue(record.getAlarmMessage().contains("低于下限"));
    }

    // ==================== 正常值不告警 ====================

    @Test
    @DisplayName("温度在阈值范围内 → 不触发告警")
    void testTemperatureWithinLimits() {
        SensorData sensorData = buildSensorData(1L, 25.0, null, null, null, null);

        AlarmRule rule = new AlarmRule();
        rule.setUpperLimit(40.0);
        rule.setLowerLimit(0.0);
        when(alarmRuleService.getRule(1L, "temperature")).thenReturn(rule);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        verify(alarmRecordMapper, never()).insert(any());
    }

    // ==================== 无规则不告警 ====================

    @Test
    @DisplayName("传感器未配置告警规则 → 跳过检查")
    void testNoRuleConfigured() {
        SensorData sensorData = buildSensorData(1L, 99.0, null, null, null, null);
        when(alarmRuleService.getRule(1L, "temperature")).thenReturn(null);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        verify(alarmRecordMapper, never()).insert(any());
    }

    // ==================== 空值跳过 ====================

    @Test
    @DisplayName("传感器值为null → 跳过该维度检查")
    void testNullValueSkipped() {
        SensorData sensorData = buildSensorData(1L, null, null, null, null, null);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        verify(alarmRuleService, never()).getRule(anyLong(), anyString());
        verify(alarmRecordMapper, never()).insert(any());
    }

    // ==================== 多传感器部分超限 ====================

    @Test
    @DisplayName("多传感器仅温度超限 → 仅触发温度告警")
    void testMultipleSensorsOnlyTemperatureAlarms() {
        SensorData sensorData = buildSensorData(1L, 50.0, 65.0, 500.0, 220.0, 5.0);

        AlarmRule tempRule = new AlarmRule();
        tempRule.setUpperLimit(40.0);
        when(alarmRuleService.getRule(1L, "temperature")).thenReturn(tempRule);

        // 其他传感器设宽阈值为正常范围
        AlarmRule normalRule = new AlarmRule();
        normalRule.setUpperLimit(9999.0);
        normalRule.setLowerLimit(-9999.0);
        when(alarmRuleService.getRule(1L, "humidity")).thenReturn(normalRule);
        when(alarmRuleService.getRule(1L, "illumination")).thenReturn(normalRule);
        when(alarmRuleService.getRule(1L, "voltage")).thenReturn(normalRule);
        when(alarmRuleService.getRule(1L, "current")).thenReturn(normalRule);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        // 仅触发一次温度告警
        verify(alarmRecordMapper, times(1)).insert(any(AlarmRecord.class));
    }

    // ==================== 仅超上限无下限规则 ====================

    @Test
    @DisplayName("仅配置上限无下限 → 只检查超上限")
    void testOnlyUpperLimitConfigured() {
        SensorData sensorData = buildSensorData(1L, 50.0, null, null, null, null);

        AlarmRule rule = new AlarmRule();
        rule.setUpperLimit(40.0);
        rule.setLowerLimit(null);  // 无下限
        when(alarmRuleService.getRule(1L, "temperature")).thenReturn(rule);

        alarmRecordService.checkAndCreateAlarm(sensorData);

        verify(alarmRecordMapper).insert(any(AlarmRecord.class));
    }

    // ==================== 查询告警记录 ====================

    @Test
    @DisplayName("处理告警 → handled 标记为 1")
    void testHandleAlarm() {
        AlarmRecord record = new AlarmRecord();
        record.setId(1L);
        record.setHandled(0);
        when(alarmRecordMapper.selectById(1L)).thenReturn(record);

        alarmRecordService.handleAlarm(1L);

        ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
        verify(alarmRecordMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getHandled());
    }

    @Test
    @DisplayName("处理不存在的告警 → 无异常")
    void testHandleAlarm_NotFound() {
        when(alarmRecordMapper.selectById(999L)).thenReturn(null);
        assertDoesNotThrow(() -> alarmRecordService.handleAlarm(999L));
        verify(alarmRecordMapper, never()).updateById(any());
    }

    // ==================== 辅助方法 ====================

    private SensorData buildSensorData(Long lampId, Double temp, Double hum,
                                        Double illum, Double volt, Double curr) {
        SensorData data = new SensorData();
        data.setLampId(lampId);
        data.setTemperature(temp);
        data.setHumidity(hum);
        data.setIllumination(illum);
        data.setVoltage(volt);
        data.setCurrent(curr);
        data.setCaptureTime(LocalDateTime.now());
        return data;
    }
}
