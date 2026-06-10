package com.edgevideoanalysis.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edgevideoanalysis.control.entity.ControlCommand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备控制指令Mapper
 */
@Mapper
public interface ControlCommandMapper extends BaseMapper<ControlCommand> {
}
