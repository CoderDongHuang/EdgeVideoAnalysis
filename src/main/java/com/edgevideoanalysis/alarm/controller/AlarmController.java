package com.edgevideoanalysis.alarm.controller;

import com.edgevideoanalysis.alarm.dto.AlarmQueryDTO;
import com.edgevideoanalysis.alarm.dto.AlarmRuleDTO;
import com.edgevideoanalysis.alarm.entity.AlarmRule;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.alarm.service.IAlarmRuleService;
import com.edgevideoanalysis.alarm.vo.AlarmRecordVO;
import com.edgevideoanalysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final IAlarmRuleService alarmRuleService;
    private final IAlarmRecordService alarmRecordService;

    @PostMapping("/rule")
    public Result<Void> setRule(@RequestBody AlarmRuleDTO dto) {
        alarmRuleService.setRule(dto);
        return Result.success();
    }

    @GetMapping("/rule/{lampId}")
    public Result<AlarmRule> getRule(@PathVariable Long lampId, @RequestParam String sensorType) {
        return Result.success(alarmRuleService.getRule(lampId, sensorType));
    }

    @GetMapping("/records")
    public Result<List<AlarmRecordVO>> queryRecords(AlarmQueryDTO queryDTO) {
        return Result.success(alarmRecordService.queryRecords(queryDTO));
    }

    @PutMapping("/handle/{alarmId}")
    public Result<Void> handleAlarm(@PathVariable Long alarmId) {
        alarmRecordService.handleAlarm(alarmId);
        return Result.success();
    }
}
