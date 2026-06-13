package com.edgevideoanalysis.sensor;

import com.alibaba.fastjson2.JSON;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.common.exception.BusinessException;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.entity.SensorData;
import com.edgevideoanalysis.sensor.mapper.SensorDataMapper;
import com.edgevideoanalysis.sensor.service.impl.SensorDataServiceImpl;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SensorDataServiceImpl 单元测试
 * 覆盖数据上报入库 / Redis 缓存读写 / 缓存命中与穿透 / 告警联动
 */
@ExtendWith(MockitoExtension.class)
class SensorDataServiceImplTest {

    @Mock
    private SensorDataMapper sensorDataMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private IAlarmRecordService alarmRecordService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SensorDataServiceImpl sensorDataService;

    // ==================== reportData ====================

    @Test
    @DisplayName("上报数据 → 入库 + 写Redis缓存 + 触发告警检查")
    void testReportData_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        SensorDataDTO dto = buildSensorDataDTO(1L, 25.0, 60.0, 800.0, 220.0, 5.0);
        sensorDataService.reportData(dto);

        // 1. 验证写库
        ArgumentCaptor<SensorData> entityCaptor = ArgumentCaptor.forClass(SensorData.class);
        verify(sensorDataMapper).insert(entityCaptor.capture());
        SensorData entity = entityCaptor.getValue();
        assertEquals(1L, entity.getLampId());
        assertEquals(25.0, entity.getTemperature());

        // 2. 验证写Redis缓存
        verify(valueOperations).set(
                eq("sensor:latest:1"),
                anyString(),
                eq(1L),
                eq(java.util.concurrent.TimeUnit.HOURS)
        );

        // 3. 验证触发告警检查
        verify(alarmRecordService).checkAndCreateAlarm(any(SensorData.class));
    }

    // ==================== getLatestData — 缓存命中 ====================

    @Test
    @DisplayName("获取最新数据 → Redis 缓存命中，直接返回")
    void testGetLatestData_CacheHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        SensorDataVO cachedVo = new SensorDataVO();
        cachedVo.setLampId(1L);
        cachedVo.setTemperature(28.0);
        cachedVo.setHumidity(55.0);
        String cachedJson = JSON.toJSONString(cachedVo);

        when(valueOperations.get("sensor:latest:1")).thenReturn(cachedJson);

        SensorDataVO result = sensorDataService.getLatestData(1L);

        assertEquals(1L, result.getLampId());
        assertEquals(28.0, result.getTemperature());
        assertEquals(55.0, result.getHumidity());

        // 验证没有查库
        verify(sensorDataMapper, never()).selectOne(any());
    }

    // ==================== getLatestData — 缓存未命中回源DB ====================

    @Test
    @DisplayName("获取最新数据 → Redis 未命中，回源DB并回写缓存")
    void testGetLatestData_CacheMiss_FallbackToDB() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Redis miss
        when(valueOperations.get("sensor:latest:1")).thenReturn(null);

        // DB hit
        SensorData dbData = buildSensorDataEntity(1L, 22.0, 50.0, 600.0, 220.0, 3.0);
        when(sensorDataMapper.selectOne(any())).thenReturn(dbData);

        SensorDataVO result = sensorDataService.getLatestData(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        assertEquals(22.0, result.getTemperature());

        // 验证回写缓存
        verify(valueOperations).set(eq("sensor:latest:1"), anyString(), eq(1L), any());
    }

    // ==================== getLatestData — 缓存和DB均无数据 ====================

    @Test
    @DisplayName("获取最新数据 → 缓存和DB均无数据，抛BusinessException")
    void testGetLatestData_NotFound() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sensor:latest:1")).thenReturn(null);
        when(sensorDataMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> sensorDataService.getLatestData(1L));
    }

    // ==================== queryHistory ====================

    @Test
    @DisplayName("查询历史数据 → 返回VO列表")
    void testQueryHistory() {
        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("lamp_id", 1L);
        row.put("temperature", 25.0);
        row.put("humidity", 60.0);
        row.put("illumination", 700.0);
        row.put("voltage", 220.0);
        row.put("current", 4.0);
        row.put("capture_time", LocalDateTime.now());
        mockResults.add(row);

        when(sensorDataMapper.queryHistoryData(eq(1L), any(), any()))
                .thenReturn(mockResults);

        SensorQueryDTO query = new SensorQueryDTO();
        query.setLampId(1L);
        query.setStartTime(LocalDateTime.now().minusHours(1));
        query.setEndTime(LocalDateTime.now());

        List<SensorDataVO> results = sensorDataService.queryHistory(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getLampId());
    }

    // ==================== getCurveData ====================

    @Test
    @DisplayName("获取曲线数据 → 返回时序VO列表")
    void testGetCurveData() {
        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> point = new HashMap<>();
        point.put("time", LocalDateTime.now());
        point.put("value", 28.5);
        mockResults.add(point);

        when(sensorDataMapper.queryCurveData(eq(1L), eq("temperature"), any(), any()))
                .thenReturn(mockResults);

        SensorQueryDTO query = new SensorQueryDTO();
        query.setLampId(1L);
        query.setSensorType("temperature");
        query.setStartTime(LocalDateTime.now().minusHours(1));
        query.setEndTime(LocalDateTime.now());

        List<SensorCurveVO> results = sensorDataService.getCurveData(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(28.5, results.get(0).getValue());
    }

    // ==================== 辅助方法 ====================

    private SensorDataDTO buildSensorDataDTO(Long lampId, Double temp, Double hum,
                                              Double illum, Double volt, Double curr) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setLampId(lampId);
        dto.setTemperature(temp);
        dto.setHumidity(hum);
        dto.setIllumination(illum);
        dto.setVoltage(volt);
        dto.setCurrent(curr);
        dto.setCaptureTime(LocalDateTime.now());
        return dto;
    }

    private SensorData buildSensorDataEntity(Long lampId, Double temp, Double hum,
                                              Double illum, Double volt, Double curr) {
        SensorData data = new SensorData();
        data.setId(1L);
        data.setLampId(lampId);
        data.setTemperature(temp);
        data.setHumidity(hum);
        data.setIllumination(illum);
        data.setVoltage(volt);
        data.setCurrent(curr);
        data.setCaptureTime(LocalDateTime.now());
        return data;
    }
}
