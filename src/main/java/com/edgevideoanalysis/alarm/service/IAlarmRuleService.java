package com.edgevideoanalysis.alarm.service;

import com.edgevideoanalysis.alarm.dto.AlarmRuleDTO;
import com.edgevideoanalysis.alarm.entity.AlarmRule;

public interface IAlarmRuleService {

    void setRule(AlarmRuleDTO dto);

    AlarmRule getRule(Long lampId, String sensorType);
}
