package com.edgevideoanalysis.device.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.device.dto.LampDTO;
import com.edgevideoanalysis.device.service.ILampService;
import com.edgevideoanalysis.device.vo.LampVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lamp")
@RequiredArgsConstructor
public class LampController {

    private final ILampService lampService;

    @GetMapping
    public Result<List<LampVO>> listLamps() {
        return Result.success(lampService.listLamps());
    }

    @GetMapping("/{id}")
    public Result<LampVO> getLampDetail(@PathVariable Long id) {
        return Result.success(lampService.getLampDetail(id));
    }

    @GetMapping("/{id}/status")
    public Result<LampVO> getLampStatus(@PathVariable Long id) {
        return Result.success(lampService.getLampStatus(id));
    }

    @PostMapping
    public Result<Void> createLamp(@RequestBody LampDTO dto) {
        lampService.createLamp(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateLamp(@PathVariable Long id, @RequestBody LampDTO dto) {
        lampService.updateLamp(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteLamp(@PathVariable Long id) {
        lampService.deleteLamp(id);
        return Result.success();
    }
}
