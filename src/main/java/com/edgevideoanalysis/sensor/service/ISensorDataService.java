package com.edgevideoanalysis.sensor.service;

import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;

import java.util.List;

public interface ISensorDataService {

    void reportData(SensorDataDTO dto);

    SensorDataVO getLatestData(Long lampId);

    List<SensorDataVO> queryHistory(SensorQueryDTO queryDTO);

    List<SensorCurveVO> getCurveData(SensorQueryDTO queryDTO);
}
