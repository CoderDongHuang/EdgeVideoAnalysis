package com.edgevideoanalysis.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edgevideoanalysis.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
