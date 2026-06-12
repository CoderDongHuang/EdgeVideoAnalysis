package com.edgevideoanalysis.ai.controller;

import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.service.IAIInferenceService;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import com.edgevideoanalysis.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "AI推理")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final IAIInferenceService aiInferenceService;

    @ApiOperation("执行AI行人检测推理")
    @PostMapping("/inference")
    public Result<InferenceResultVO> inference(@RequestBody InferenceRequestDTO request) {
        InferenceResultVO result = aiInferenceService.inference(request);
        return Result.success(result);
    }

    @ApiOperation("查询推理记录列表")
    @GetMapping("/records")
    public Result<List<InferenceRecord>> queryRecords(InferenceQueryDTO query) {
        List<InferenceRecord> records = aiInferenceService.queryRecords(query);
        return Result.success(records);
    }

    @ApiOperation("获取推理记录详情")
    @GetMapping("/records/{id}")
    public Result<InferenceRecord> getRecordDetail(
            @ApiParam("推理记录ID") @PathVariable Long id) {
        InferenceRecord record = aiInferenceService.getRecordById(id);
        if (record == null) {
            return Result.error("推理记录不存在");
        }
        return Result.success(record);
    }
}
