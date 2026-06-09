package com.edgevideoanalysis.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edgevideoanalysis.ai.entity.InferenceRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InferenceRecordMapper extends BaseMapper<InferenceRecord> {
}
