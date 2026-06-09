package com.edgevideoanalysis.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.mapper.InferenceRecordMapper;
import com.edgevideoanalysis.ai.service.IAIInferenceService;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YOLOv8推理服务实现
 * 通过HTTP接口调用YOLOv8推理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YOLOv8InferenceServiceImpl implements IAIInferenceService {

    private final InferenceRecordMapper inferenceRecordMapper;

    private final RestTemplate restTemplate;

    @Value("${ai.yolov8.url:http://localhost:8000/predict}")
    private String yolov8Url;

    @Override
    public InferenceResultVO inference(InferenceRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image", request.getImage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        // 调用YOLOv8 HTTP接口
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(yolov8Url, HttpMethod.POST, httpEntity, String.class);
        } catch (Exception e) {
            log.error("调用YOLOv8推理接口失败: {}", yolov8Url, e);
            return buildErrorResult(request);
        }

        // 解析返回结果
        JSONObject resultJson = JSON.parseObject(response.getBody());
        long inferenceTime = System.currentTimeMillis() - startTime;

        // 构建推理结果VO
        InferenceResultVO vo = new InferenceResultVO();
        vo.setLampId(request.getLampId());
        vo.setOriginalImage(request.getImage());
        vo.setProcessedImage(resultJson.getString("processed_image"));
        vo.setPersonCount(resultJson.getInteger("person_count"));
        vo.setInferenceResults(resultJson.getString("inference_results"));
        vo.setInferenceTime(inferenceTime);

        // 存储推理记录
        saveInferenceRecord(request, vo, inferenceTime);

        log.info("AI推理完成: lampId={}, 耗时={}ms, 检测人数={}",
                request.getLampId(), inferenceTime, vo.getPersonCount());

        return vo;
    }

    @Override
    public List<InferenceRecord> queryRecords(InferenceQueryDTO query) {
        return inferenceRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InferenceRecord>()
                        .eq(query.getLampId() != null, InferenceRecord::getLampId, query.getLampId())
                        .between(query.getStartTime() != null && query.getEndTime() != null,
                                InferenceRecord::getCreateTime, query.getStartTime(), query.getEndTime())
                        .orderByDesc(InferenceRecord::getCreateTime)
        );
    }

    @Override
    public InferenceRecord getRecordById(Long id) {
        return inferenceRecordMapper.selectById(id);
    }

    private void saveInferenceRecord(InferenceRequestDTO request, InferenceResultVO vo, long inferenceTime) {
        try {
            InferenceRecord record = new InferenceRecord();
            record.setLampId(request.getLampId());
            record.setOriginalImage(request.getImage());
            record.setProcessedImage(vo.getProcessedImage());
            record.setPersonCount(vo.getPersonCount());
            record.setInferenceResults(vo.getInferenceResults());
            record.setInferenceTime(inferenceTime);
            record.setCreateTime(LocalDateTime.now());
            inferenceRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("存储推理记录失败: lampId={}", request.getLampId(), e);
        }
    }

    private InferenceResultVO buildErrorResult(InferenceRequestDTO request) {
        InferenceResultVO vo = new InferenceResultVO();
        vo.setLampId(request.getLampId());
        vo.setOriginalImage(request.getImage());
        vo.setPersonCount(0);
        vo.setInferenceResults("[]");
        vo.setInferenceTime(0L);
        return vo;
    }
}
