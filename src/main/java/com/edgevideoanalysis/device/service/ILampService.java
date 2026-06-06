package com.edgevideoanalysis.device.service;

import com.edgevideoanalysis.device.dto.LampDTO;
import com.edgevideoanalysis.device.vo.LampVO;

import java.util.List;

public interface ILampService {

    List<LampVO> listLamps();

    LampVO getLampDetail(Long id);

    LampVO getLampStatus(Long id);

    void createLamp(LampDTO dto);

    void updateLamp(Long id, LampDTO dto);

    void deleteLamp(Long id);
}
