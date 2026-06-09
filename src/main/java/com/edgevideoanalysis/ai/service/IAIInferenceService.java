package com.edgevideoanalysis.ai.service;

import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;

import java.util.List;

public interface IAIInferenceService {

    /**
     * 执行AI推理
     * @param request 推理请求DTO
     * @return 推理结果VO
     */
    InferenceResultVO inference(InferenceRequestDTO request);

    /**
     * 查询推理记录
     * @param query 查询条件DTO
     * @return 推理记录列表
     */
    List<InferenceRecord> queryRecords(InferenceQueryDTO query);

    /**
     * 根据ID获取推理记录
     * @param id 记录ID
     * @return 推理记录
     */
    InferenceRecord getRecordById(Long id);
}
