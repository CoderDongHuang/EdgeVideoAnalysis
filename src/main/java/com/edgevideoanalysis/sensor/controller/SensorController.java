package com.edgevideoanalysis.sensor.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.service.ISensorDataService;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "传感器数据")
@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final ISensorDataService sensorDataService;

    @ApiOperation("上报传感器数据")
    @PostMapping("/report")
    public Result<Void> reportData(@RequestBody SensorDataDTO dto) {
        sensorDataService.reportData(dto);
        return Result.success();
    }

    @ApiOperation("获取灯杆最新传感器数据")
    @GetMapping("/{lampId}/latest")
    public Result<SensorDataVO> getLatestData(
            @ApiParam("灯杆ID") @PathVariable Long lampId) {
        return Result.success(sensorDataService.getLatestData(lampId));
    }

    @ApiOperation("查询传感器历史数据")
    @GetMapping("/history")
    public Result<List<SensorDataVO>> queryHistory(SensorQueryDTO queryDTO) {
        return Result.success(sensorDataService.queryHistory(queryDTO));
    }

    @ApiOperation("获取传感器曲线数据")
    @GetMapping("/curve")
    public Result<List<SensorCurveVO>> getCurveData(SensorQueryDTO queryDTO) {
        return Result.success(sensorDataService.getCurveData(queryDTO));
    }
}
