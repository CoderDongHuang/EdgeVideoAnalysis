package com.edgevideoanalysis.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edgevideoanalysis.alarm.dto.AlarmRuleDTO;
import com.edgevideoanalysis.alarm.entity.AlarmRule;
import com.edgevideoanalysis.alarm.mapper.AlarmRuleMapper;
import com.edgevideoanalysis.alarm.service.IAlarmRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmRuleServiceImpl implements IAlarmRuleService {

    private final AlarmRuleMapper alarmRuleMapper;

    @Override
    public void setRule(AlarmRuleDTO dto) {
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRule::getLampId, dto.getLampId())
               .eq(AlarmRule::getSensorType, dto.getSensorType());
        AlarmRule existingRule = alarmRuleMapper.selectOne(wrapper);

        if (existingRule != null) {
            existingRule.setUpperLimit(dto.getUpperLimit());
            existingRule.setLowerLimit(dto.getLowerLimit());
            existingRule.setEnabled(dto.getEnabled());
            alarmRuleMapper.updateById(existingRule);
        } else {
            AlarmRule newRule = new AlarmRule();
            newRule.setLampId(dto.getLampId());
            newRule.setSensorType(dto.getSensorType());
            newRule.setUpperLimit(dto.getUpperLimit());
            newRule.setLowerLimit(dto.getLowerLimit());
            newRule.setEnabled(dto.getEnabled());
            alarmRuleMapper.insert(newRule);
        }
    }

    @Override
    public AlarmRule getRule(Long lampId, String sensorType) {
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRule::getLampId, lampId)
               .eq(AlarmRule::getSensorType, sensorType)
               .eq(AlarmRule::getEnabled, 1);
        return alarmRuleMapper.selectOne(wrapper);
    }
}
