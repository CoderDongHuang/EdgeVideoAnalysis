package com.edgevideoanalysis.alarm.service;

import com.edgevideoanalysis.alarm.dto.AlarmQueryDTO;
import com.edgevideoanalysis.alarm.vo.AlarmRecordVO;
import com.edgevideoanalysis.sensor.entity.SensorData;

import java.util.List;

public interface IAlarmRecordService {

    void checkAndCreateAlarm(SensorData sensorData);

    List<AlarmRecordVO> queryRecords(AlarmQueryDTO queryDTO);

    void handleAlarm(Long alarmId);
}
