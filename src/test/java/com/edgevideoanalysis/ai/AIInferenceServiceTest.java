package com.edgevideoanalysis.ai;

import com.edgevideoanalysis.ai.dto.InferenceQueryDTO;
import com.edgevideoanalysis.ai.dto.InferenceRequestDTO;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import com.edgevideoanalysis.ai.mapper.InferenceRecordMapper;
import com.edgevideoanalysis.ai.service.impl.YOLOv8InferenceServiceImpl;
import com.edgevideoanalysis.ai.vo.InferenceResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AIInferenceServiceTest {

    @Autowired
    private YOLOv8InferenceServiceImpl aiInferenceService;

    @Autowired
    private InferenceRecordMapper inferenceRecordMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void testQueryRecords() {
        // Insert test data
        InferenceRecord record = new InferenceRecord();
        record.setLampId(1L);
        record.setOriginalImage("test");
        record.setPersonCount(2);
        record.setInferenceTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        inferenceRecordMapper.insert(record);

        // Query
        InferenceQueryDTO query = new InferenceQueryDTO();
        query.setLampId(1L);
        
        var records = aiInferenceService.queryRecords(query);
        
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals(1L, records.get(0).getLampId());

        // Cleanup
        inferenceRecordMapper.deleteById(record.getId());
    }

    @Test
    public void testGetRecordById() {
        // Insert test data
        InferenceRecord record = new InferenceRecord();
        record.setLampId(1L);
        record.setOriginalImage("test");
        record.setPersonCount(1);
        record.setInferenceTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        inferenceRecordMapper.insert(record);

        // Get by ID
        InferenceRecord found = aiInferenceService.getRecordById(record.getId());

        assertNotNull(found);
        assertEquals(record.getId(), found.getId());
        assertEquals(1L, found.getLampId());

        // Cleanup
        inferenceRecordMapper.deleteById(record.getId());
    }

    @Test
    public void testGetRecordById_NotFound() {
        InferenceRecord found = aiInferenceService.getRecordById(99999L);
        assertNull(found);
    }
}
