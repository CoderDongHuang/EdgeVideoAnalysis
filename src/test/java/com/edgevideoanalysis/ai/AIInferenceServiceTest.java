package com.edgevideoanalysis.ai;

import com.alibaba.fastjson2.JSONObject;
import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.mapper.InferenceRecordMapper;
import com.edgevideoanalysis.ai.service.impl.YOLOv8InferenceServiceImpl;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * YOLOv8InferenceServiceImpl 纯 Mockito 单元测试
 * 无需 Spring 容器，覆盖推理调用/HTTP失败降级/记录查询
 */
@ExtendWith(MockitoExtension.class)
class AIInferenceServiceTest {

    @Mock
    private InferenceRecordMapper inferenceRecordMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private YOLOv8InferenceServiceImpl aiInferenceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiInferenceService, "yolov8Url", "http://yolov8:5000/inference");
    }

    // ==================== 推理成功 ====================

    @Test
    @DisplayName("推理成功 → 解析YOLO响应 → 存库 → 返回VO")
    void testInference_Success() {
        InferenceRequestDTO request = buildRequest(1L, "base64image");

        // 模拟 YOLOv8 Flask 响应
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("person_count", 3);
        mockResponse.put("processed_image", "base64processed");
        mockResponse.put("inference_results", "[{\"class\":\"person\",\"confidence\":0.95}]");

        when(restTemplate.exchange(eq("http://yolov8:5000/inference"),
                eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse.toJSONString(), HttpStatus.OK));

        InferenceResultVO result = aiInferenceService.inference(request);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        assertEquals("base64image", result.getOriginalImage());
        assertEquals(3, result.getPersonCount());
        assertEquals("base64processed", result.getProcessedImage());
        assertTrue(result.getInferenceTime() >= 0);

        // 验证保存推理记录
        verify(inferenceRecordMapper).insert(any(InferenceRecord.class));
    }

    // ==================== HTTP 调用失败 → 降级返回 ====================

    @Test
    @DisplayName("YOLO服务不可达 → 返回降级结果 personCount=0")
    void testInference_HttpFailure() {
        InferenceRequestDTO request = buildRequest(1L, "base64image");

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        InferenceResultVO result = aiInferenceService.inference(request);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        assertEquals(0, result.getPersonCount());
        assertEquals("[]", result.getInferenceResults());
        assertEquals(0L, result.getInferenceTime());

        // 不保存记录
        verify(inferenceRecordMapper, never()).insert(any());
    }

    // ==================== 查询推理记录 ====================

    @Test
    @DisplayName("查询推理记录 → 返回列表")
    void testQueryRecords() {
        InferenceQueryDTO query = new InferenceQueryDTO();
        query.setLampId(1L);

        List<InferenceRecord> mockList = List.of(new InferenceRecord());
        when(inferenceRecordMapper.selectList(any())).thenReturn(mockList);

        List<InferenceRecord> results = aiInferenceService.queryRecords(query);
        assertEquals(1, results.size());
    }

    // ==================== 按ID查询 ====================

    @Test
    @DisplayName("按ID查询推理记录 → 存在")
    void testGetRecordById_Found() {
        InferenceRecord record = new InferenceRecord();
        record.setId(100L);
        when(inferenceRecordMapper.selectById(100L)).thenReturn(record);

        InferenceRecord result = aiInferenceService.getRecordById(100L);
        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    @DisplayName("按ID查询推理记录 → 不存在")
    void testGetRecordById_NotFound() {
        when(inferenceRecordMapper.selectById(999L)).thenReturn(null);
        assertNull(aiInferenceService.getRecordById(999L));
    }

    private InferenceRequestDTO buildRequest(Long lampId, String image) {
        InferenceRequestDTO request = new InferenceRequestDTO();
        request.setLampId(lampId);
        request.setImage(image);
        return request;
    }
}
