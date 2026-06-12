package com.edgevideoanalysis.video.controller;

import com.edgevideoanalysis.common.result.Result;
import com.edgevideoanalysis.video.dto.VideoFrameDTO;
import com.edgevideoanalysis.video.service.IVideoStreamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "视频流处理")
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoStreamService videoStreamService;

    @ApiOperation("获取灯杆当前视频帧（Base64）")
    @GetMapping("/frame/{lampId}")
    public Result<VideoFrameDTO> getCurrentFrame(
            @ApiParam("灯杆ID") @PathVariable Long lampId) {
        VideoFrameDTO frame = videoStreamService.getCurrentFrame(lampId);
        if (frame == null) {
            return Result.error("获取视频帧失败");
        }
        return Result.success(frame);
    }

    @ApiOperation("获取设备视频流地址")
    @GetMapping("/stream/{lampId}")
    public Result<String> getStreamUrl(
            @ApiParam("灯杆ID") @PathVariable Long lampId) {
        String streamUrl = videoStreamService.getStreamUrl(lampId);
        if (streamUrl == null) {
            return Result.error("灯杆不存在或未配置视频流地址");
        }
        return Result.success(streamUrl);
    }
}
