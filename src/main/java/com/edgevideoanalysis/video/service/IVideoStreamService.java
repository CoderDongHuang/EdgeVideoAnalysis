package com.edgevideoanalysis.video.service;

import com.edgevideoanalysis.video.dto.VideoFrameDTO;

public interface IVideoStreamService {

    /**
     * 获取某灯杆当前帧图片
     * @param lampId 灯杆ID
     * @return 视频帧DTO
     */
    VideoFrameDTO getCurrentFrame(Long lampId);

    /**
     * 获取视频流地址
     * @param lampId 灯杆ID
     * @return 视频流URL
     */
    String getStreamUrl(Long lampId);
}
