package com.edgevideoanalysis.sensor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edgevideoanalysis.sensor.entity.SensorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SensorDataMapper extends BaseMapper<SensorData> {

    List<Map<String, Object>> queryHistoryData(
            @Param("lampId") Long lampId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<Map<String, Object>> queryCurveData(
            @Param("lampId") Long lampId,
            @Param("sensorType") String sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
