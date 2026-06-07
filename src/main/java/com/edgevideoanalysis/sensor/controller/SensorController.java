package com.edgevideoanalysis.sensor.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.service.ISensorDataService;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final ISensorDataService sensorDataService;

    @PostMapping("/report")
    public Result<Void> reportData(@RequestBody SensorDataDTO dto) {
        sensorDataService.reportData(dto);
        return Result.success();
    }

    @GetMapping("/{lampId}/latest")
    public Result<SensorDataVO> getLatestData(@PathVariable Long lampId) {
        return Result.success(sensorDataService.getLatestData(lampId));
    }

    @GetMapping("/history")
    public Result<List<SensorDataVO>> queryHistory(SensorQueryDTO queryDTO) {
        return Result.success(sensorDataService.queryHistory(queryDTO));
    }

    @GetMapping("/curve")
    public Result<List<SensorCurveVO>> getCurveData(SensorQueryDTO queryDTO) {
        return Result.success(sensorDataService.getCurveData(queryDTO));
    }
}
