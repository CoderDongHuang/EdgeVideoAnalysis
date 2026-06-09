package com.edgevideoanalysis.video.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.video.dto.VideoFrameDTO;
import com.edgevideoanalysis.video.service.IVideoStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoStreamService videoStreamService;

    /**
     * 获取某灯杆当前帧图片
     */
    @GetMapping("/frame/{lampId}")
    public Result<VideoFrameDTO> getCurrentFrame(@PathVariable Long lampId) {
        VideoFrameDTO frame = videoStreamService.getCurrentFrame(lampId);
        if (frame == null) {
            return Result.error("获取视频帧失败");
        }
        return Result.success(frame);
    }

    /**
     * 获取视频流地址
     */
    @GetMapping("/stream/{lampId}")
    public Result<String> getStreamUrl(@PathVariable Long lampId) {
        String streamUrl = videoStreamService.getStreamUrl(lampId);
        if (streamUrl == null) {
            return Result.error("灯杆不存在或未配置视频流地址");
        }
        return Result.success(streamUrl);
    }
}
