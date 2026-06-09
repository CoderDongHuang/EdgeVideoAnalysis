package com.edgevideoanalysis.ai.service;

import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI推理适配器
 * 采用适配器模式统一推理入口，屏蔽底层YOLOv8实现细节
 */
@Component
@RequiredArgsConstructor
public class AIInferenceAdapter implements IAIInferenceService {

    private final IAIInferenceService aiInferenceService;

    @Override
    public InferenceResultVO inference(InferenceRequestDTO request) {
        return aiInferenceService.inference(request);
    }

    @Override
    public List<InferenceRecord> queryRecords(InferenceQueryDTO query) {
        return aiInferenceService.queryRecords(query);
    }

    @Override
    public InferenceRecord getRecordById(Long id) {
        return aiInferenceService.getRecordById(id);
    }
}
