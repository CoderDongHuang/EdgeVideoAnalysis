package com.edgevideoanalysis.sensor;

import com.alibaba.fastjson2.JSON;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.common.exception.BusinessException;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.entity.SensorData;
import com.edgevideoanalysis.sensor.mapper.SensorDataMapper;
import com.edgevideoanalysis.sensor.service.SensorDataProducer;
import com.edgevideoanalysis.sensor.service.impl.SensorDataServiceImpl;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private SensorDataProducer sensorDataProducer;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SensorDataServiceImpl sensorDataService;

    // ==================== reportData → MQ异步 ====================

    @Test
    @DisplayName("上报数据 → MQ投递成功 → 不执行同步写库")
    void testReportData_MQSuccess() {
        when(sensorDataProducer.sendSensorData(any(SensorDataDTO.class))).thenReturn(true);

        SensorDataDTO dto = buildSensorDataDTO(1L, 25.0, 60.0, 800.0, 220.0, 5.0);
        sensorDataService.reportData(dto);

        // 1. 验证投递到 MQ
        verify(sensorDataProducer).sendSensorData(dto);

        // 2. MQ 成功后不触发同步写库
        verify(sensorDataMapper, never()).insert(any());
        verify(alarmRecordService, never()).checkAndCreateAlarm(any());
    }

    // ==================== reportData → MQ降级兜底 ====================

    @Test
    @DisplayName("MQ投递失败 → 降级同步处理（写库+缓存+告警）")
    void testReportData_MQFallbackSync() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(sensorDataProducer.sendSensorData(any(SensorDataDTO.class))).thenReturn(false);

        SensorDataDTO dto = buildSensorDataDTO(1L, 25.0, 60.0, 800.0, 220.0, 5.0);
        sensorDataService.reportData(dto);

        // 1. 验证尝试了 MQ
        verify(sensorDataProducer).sendSensorData(dto);

        // 2. 降级后执行同步写库
        verify(sensorDataMapper).insert(any(SensorData.class));

        // 3. 降级后写 Redis 缓存
        verify(valueOperations).set(eq("sensor:latest:1"), anyString(), anyLong(),
                any(TimeUnit.class));

        // 4. 降级后触发告警检查
        verify(alarmRecordService).checkAndCreateAlarm(any(SensorData.class));
    }

    // ==================== getLatestData — 缓存命中 ====================

    @Test
    @DisplayName("缓存命中 → 直接返回，不查库不加锁")
    void testGetLatestData_CacheHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        SensorDataVO cachedVo = new SensorDataVO();
        cachedVo.setLampId(1L);
        cachedVo.setTemperature(28.0);
        String cachedJson = JSON.toJSONString(cachedVo);
        when(valueOperations.get("sensor:latest:1")).thenReturn(cachedJson);

        SensorDataVO result = sensorDataService.getLatestData(1L);

        assertEquals(28.0, result.getTemperature());
        verify(sensorDataMapper, never()).selectOne(any());
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), anyLong(), any());
    }

    // ==================== 缓存穿透防护 → NULL_MARKER ====================

    @Test
    @DisplayName("缓存命中NULL_MARKER → 防穿透，直接抛异常不查库")
    void testGetLatestData_NullMarkerHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sensor:latest:1")).thenReturn("__NULL__");

        assertThrows(BusinessException.class, () -> sensorDataService.getLatestData(1L));
        // 不应该查库
        verify(sensorDataMapper, never()).selectOne(any());
    }

    // ==================== 缓存未命中 → 获锁 → 重建 ====================

    @Test
    @DisplayName("缓存未命中+获锁成功 → 查DB回写缓存，释放锁")
    void testGetLatestData_CacheMiss_Rebuild() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // 第一次 get 返回 null（缓存未命中）
        when(valueOperations.get("sensor:latest:1")).thenReturn(null);

        // 获取分布式锁成功
        when(valueOperations.setIfAbsent(eq("sensor:lock:1"), eq("1"), anyLong(), any()))
                .thenReturn(true);

        // 双重检查：获锁后再查缓存仍为 null
        // DB 有数据
        SensorData dbData = buildSensorDataEntity(1L, 22.0, 50.0, 600.0, 220.0, 3.0);
        when(sensorDataMapper.selectOne(any())).thenReturn(dbData);

        SensorDataVO result = sensorDataService.getLatestData(1L);

        assertNotNull(result);
        assertEquals(22.0, result.getTemperature());

        // 验证回写缓存 + 释放锁
        verify(valueOperations).set(eq("sensor:latest:1"), anyString(), anyLong(), any());
        verify(stringRedisTemplate).delete("sensor:lock:1");
    }

    // ==================== 缓存未命中 → 获锁 → DB无数据 → 缓存NULL_MARKER ====================

    @Test
    @DisplayName("缓存未命中+获锁 → DB无数据 → 缓存NULL_MARKER防穿透")
    void testGetLatestData_CacheMiss_DBNull() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sensor:latest:1")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("sensor:lock:1"), eq("1"), anyLong(), any()))
                .thenReturn(true);
        when(sensorDataMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> sensorDataService.getLatestData(1L));

        // 验证缓存了空值标记
        verify(valueOperations).set(eq("sensor:latest:1"), eq("__NULL__"), anyLong(), any());
        verify(stringRedisTemplate).delete("sensor:lock:1");
    }

    // ==================== 缓存击穿防护 → 锁竞争 → 自旋重试 ====================

    @Test
    @DisplayName("锁被占用 → 自旋等待 → 缓存被其他线程重建后命中")
    void testGetLatestData_LockContention_RetrySuccess() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // 第一次 get 返回 null
        when(valueOperations.get("sensor:latest:1")).thenReturn(null);

        // 获取锁失败（其他线程正在重建）
        when(valueOperations.setIfAbsent(eq("sensor:lock:1"), eq("1"), anyLong(), any()))
                .thenReturn(false);

        // 自旋重试第1次：缓存已恢复
        SensorDataVO rebuilt = new SensorDataVO();
        rebuilt.setLampId(1L);
        rebuilt.setTemperature(30.0);
        when(valueOperations.get("sensor:latest:1"))
                .thenReturn(null)         // 第一查：miss
                .thenReturn(JSON.toJSONString(rebuilt)); // 重试：hit

        SensorDataVO result = sensorDataService.getLatestData(1L);

        assertEquals(30.0, result.getTemperature());
        // 没有直接查DB（因为锁竞争成功等待到了缓存）
        verify(sensorDataMapper, never()).selectOne(any());
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
