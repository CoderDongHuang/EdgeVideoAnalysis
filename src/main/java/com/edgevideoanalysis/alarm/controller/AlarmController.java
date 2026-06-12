package com.edgevideoanalysis.alarm.controller;

import com.edgevideoanalysis.alarm.dto.AlarmQueryDTO;
import com.edgevideoanalysis.alarm.dto.AlarmRuleDTO;
import com.edgevideoanalysis.alarm.entity.AlarmRule;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.alarm.service.IAlarmRuleService;
import com.edgevideoanalysis.alarm.vo.AlarmRecordVO;
import com.edgevideoanalysis.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "告警管理")
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final IAlarmRuleService alarmRuleService;
    private final IAlarmRecordService alarmRecordService;

    @ApiOperation("设置告警规则")
    @PostMapping("/rule")
    public Result<Void> setRule(@RequestBody AlarmRuleDTO dto) {
        alarmRuleService.setRule(dto);
        return Result.success();
    }

    @ApiOperation("查询告警规则")
    @GetMapping("/rule/{lampId}")
    public Result<AlarmRule> getRule(
            @ApiParam("灯杆ID") @PathVariable Long lampId,
            @ApiParam("传感器类型") @RequestParam String sensorType) {
        return Result.success(alarmRuleService.getRule(lampId, sensorType));
    }

    @ApiOperation("查询告警记录")
    @GetMapping("/records")
    public Result<List<AlarmRecordVO>> queryRecords(AlarmQueryDTO queryDTO) {
        return Result.success(alarmRecordService.queryRecords(queryDTO));
    }

    @ApiOperation("处理告警")
    @PutMapping("/handle/{alarmId}")
    public Result<Void> handleAlarm(
            @ApiParam("告警记录ID") @PathVariable Long alarmId) {
        alarmRecordService.handleAlarm(alarmId);
        return Result.success();
    }
}
