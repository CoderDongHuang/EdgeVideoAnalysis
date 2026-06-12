package com.edgevideoanalysis.device.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.device.dto.LampDTO;
import com.edgevideoanalysis.device.service.ILampService;
import com.edgevideoanalysis.device.vo.LampVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "设备管理")
@RestController
@RequestMapping("/api/lamp")
@RequiredArgsConstructor
public class LampController {

    private final ILampService lampService;

    @ApiOperation("获取灯杆列表")
    @GetMapping("/list")
    public Result<List<LampVO>> listLamps() {
        return Result.success(lampService.listLamps());
    }

    @ApiOperation("获取灯杆详情")
    @GetMapping("/detail/{id}")
    public Result<LampVO> getLampDetail(
            @ApiParam("灯杆ID") @PathVariable Long id) {
        return Result.success(lampService.getLampDetail(id));
    }

    @ApiOperation("获取灯杆在线状态")
    @GetMapping("/{id}/status")
    public Result<LampVO> getLampStatus(
            @ApiParam("灯杆ID") @PathVariable Long id) {
        return Result.success(lampService.getLampStatus(id));
    }

    @ApiOperation("创建设备")
    @PostMapping
    public Result<Void> createLamp(@RequestBody LampDTO dto) {
        lampService.createLamp(dto);
        return Result.success();
    }

    @ApiOperation("更新设备信息")
    @PutMapping("/{id}")
    public Result<Void> updateLamp(
            @ApiParam("灯杆ID") @PathVariable Long id,
            @RequestBody LampDTO dto) {
        lampService.updateLamp(id, dto);
        return Result.success();
    }

    @ApiOperation("删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> deleteLamp(
            @ApiParam("灯杆ID") @PathVariable Long id) {
        lampService.deleteLamp(id);
        return Result.success();
    }
}
