package com.edgevideoanalysis.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edgevideoanalysis.common.exception.BusinessException;
import com.edgevideoanalysis.device.dto.LampDTO;
import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import com.edgevideoanalysis.device.service.ILampService;
import com.edgevideoanalysis.device.vo.LampVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LampServiceImpl implements ILampService {

    private final LampMapper lampMapper;

    @Override
    public List<LampVO> listLamps() {
        List<Lamp> lamps = lampMapper.selectList(null);
        return lamps.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public LampVO getLampDetail(Long id) {
        Lamp lamp = lampMapper.selectById(id);
        if (lamp == null) {
            throw new BusinessException("灯杆设备不存在");
        }
        return convertToVO(lamp);
    }

    @Override
    public LampVO getLampStatus(Long id) {
        Lamp lamp = lampMapper.selectById(id);
        if (lamp == null) {
            throw new BusinessException("灯杆设备不存在");
        }
        LampVO vo = new LampVO();
        vo.setId(lamp.getId());
        vo.setLampCode(lamp.getLampCode());
        vo.setLedStatus(lamp.getLedStatus());
        vo.setOnlineStatus(lamp.getOnlineStatus());
        return vo;
    }

    @Override
    public void createLamp(LampDTO dto) {
        LambdaQueryWrapper<Lamp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lamp::getLampCode, dto.getLampCode());
        if (lampMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("灯杆编号已存在");
        }

        Lamp lamp = new Lamp();
        lamp.setLampCode(dto.getLampCode());
        lamp.setLampName(dto.getLampName());
        lamp.setLocation(dto.getLocation());
        lamp.setCameraUrl(dto.getCameraUrl());
        lamp.setLedStatus(0);
        lamp.setOnlineStatus(0);
        lampMapper.insert(lamp);
    }

    @Override
    public void updateLamp(Long id, LampDTO dto) {
        Lamp lamp = lampMapper.selectById(id);
        if (lamp == null) {
            throw new BusinessException("灯杆设备不存在");
        }

        lamp.setLampName(dto.getLampName());
        lamp.setLocation(dto.getLocation());
        lamp.setCameraUrl(dto.getCameraUrl());
        lampMapper.updateById(lamp);
    }

    @Override
    public void deleteLamp(Long id) {
        Lamp lamp = lampMapper.selectById(id);
        if (lamp == null) {
            throw new BusinessException("灯杆设备不存在");
        }
        lampMapper.deleteById(id);
    }

    private LampVO convertToVO(Lamp lamp) {
        LampVO vo = new LampVO();
        vo.setId(lamp.getId());
        vo.setLampCode(lamp.getLampCode());
        vo.setLampName(lamp.getLampName());
        vo.setLocation(lamp.getLocation());
        vo.setCameraUrl(lamp.getCameraUrl());
        vo.setLedStatus(lamp.getLedStatus());
        vo.setOnlineStatus(lamp.getOnlineStatus());
        vo.setCreateTime(lamp.getCreateTime());
        return vo;
    }
}
