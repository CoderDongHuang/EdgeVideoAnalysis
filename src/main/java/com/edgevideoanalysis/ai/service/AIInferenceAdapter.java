package com.edgevideoanalysis.ai.service;

import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.service.impl.YOLOv8InferenceServiceImpl;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI推理适配器
 * 采用适配器模式统一推理入口，屏蔽底层YOLOv8实现细节
 */
@Primary
@Component
@RequiredArgsConstructor
public class AIInferenceAdapter implements IAIInferenceService {

    private final YOLOv8InferenceServiceImpl yolov8InferenceService;

    @Override
    public InferenceResultVO inference(InferenceRequestDTO request) {
        return yolov8InferenceService.inference(request);
    }

    @Override
    public List<InferenceRecord> queryRecords(InferenceQueryDTO query) {
        return yolov8InferenceService.queryRecords(query);
    }

    @Override
    public InferenceRecord getRecordById(Long id) {
        return yolov8InferenceService.getRecordById(id);
    }
}
