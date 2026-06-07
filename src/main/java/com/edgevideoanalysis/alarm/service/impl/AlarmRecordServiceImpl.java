package com.edgevideoanalysis.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edgevideoanalysis.alarm.dto.AlarmQueryDTO;
import com.edgevideoanalysis.alarm.entity.AlarmRecord;
import com.edgevideoanalysis.alarm.entity.AlarmRule;
import com.edgevideoanalysis.alarm.mapper.AlarmRecordMapper;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.alarm.service.IAlarmRuleService;
import com.edgevideoanalysis.alarm.vo.AlarmRecordVO;
import com.edgevideoanalysis.sensor.entity.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlarmRecordServiceImpl implements IAlarmRecordService {

    private final AlarmRecordMapper alarmRecordMapper;
    private final IAlarmRuleService alarmRuleService;

    @Override
    public void checkAndCreateAlarm(SensorData sensorData) {
        checkSensor(sensorData, "temperature", sensorData.getTemperature());
        checkSensor(sensorData, "humidity", sensorData.getHumidity());
        checkSensor(sensorData, "illumination", sensorData.getIllumination());
        checkSensor(sensorData, "voltage", sensorData.getVoltage());
        checkSensor(sensorData, "current", sensorData.getCurrent());
    }

    private void checkSensor(SensorData sensorData, String sensorType, Double value) {
        if (value == null) {
            return;
        }

        AlarmRule rule = alarmRuleService.getRule(sensorData.getLampId(), sensorType);
        if (rule == null) {
            return;
        }

        boolean isAlarm = false;
        String message = "";

        if (rule.getUpperLimit() != null && value > rule.getUpperLimit()) {
            isAlarm = true;
            message = String.format("%s超过上限: %.2f > %.2f", sensorType, value, rule.getUpperLimit());
        } else if (rule.getLowerLimit() != null && value < rule.getLowerLimit()) {
            isAlarm = true;
            message = String.format("%s低于下限: %.2f < %.2f", sensorType, value, rule.getLowerLimit());
        }

        if (isAlarm) {
            AlarmRecord record = new AlarmRecord();
            record.setLampId(sensorData.getLampId());
            record.setSensorType(sensorType);
            record.setSensorValue(value);
            record.setAlarmLevel(1);
            record.setAlarmMessage(message);
            record.setAlarmTime(LocalDateTime.now());
            record.setHandled(0);
            alarmRecordMapper.insert(record);
        }
    }

    @Override
    public List<AlarmRecordVO> queryRecords(AlarmQueryDTO queryDTO) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDTO.getLampId() != null, AlarmRecord::getLampId, queryDTO.getLampId())
               .ge(queryDTO.getStartTime() != null, AlarmRecord::getAlarmTime, queryDTO.getStartTime())
               .le(queryDTO.getEndTime() != null, AlarmRecord::getAlarmTime, queryDTO.getEndTime())
               .eq(queryDTO.getHandled() != null, AlarmRecord::getHandled, queryDTO.getHandled())
               .orderByDesc(AlarmRecord::getAlarmTime);

        List<AlarmRecord> records = alarmRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void handleAlarm(Long alarmId) {
        AlarmRecord record = alarmRecordMapper.selectById(alarmId);
        if (record != null) {
            record.setHandled(1);
            alarmRecordMapper.updateById(record);
        }
    }

    private AlarmRecordVO convertToVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        vo.setId(record.getId());
        vo.setLampId(record.getLampId());
        vo.setSensorType(record.getSensorType());
        vo.setSensorValue(record.getSensorValue());
        vo.setAlarmLevel(record.getAlarmLevel());
        vo.setAlarmMessage(record.getAlarmMessage());
        vo.setAlarmTime(record.getAlarmTime());
        vo.setHandled(record.getHandled());
        return vo;
    }
}
