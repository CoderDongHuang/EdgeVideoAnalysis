package com.edgevideoanalysis.video.service.impl;

import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import com.edgevideoanalysis.video.dto.VideoFrameDTO;
import com.edgevideoanalysis.video.handler.VideoFrameHandler;
import com.edgevideoanalysis.video.service.IVideoStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStreamServiceImpl implements IVideoStreamService {

    private final LampMapper lampMapper;

    private final VideoFrameHandler videoFrameHandler;

    private final ThreadPoolExecutor videoTaskExecutor;

    @Override
    public VideoFrameDTO getCurrentFrame(Long lampId) {
        // 查询灯杆信息获取摄像头URL
        Lamp lamp = lampMapper.selectById(lampId);
        if (lamp == null || lamp.getCameraUrl() == null) {
            log.warn("灯杆不存在或未配置摄像头URL: lampId={}", lampId);
            return null;
        }

        String cameraUrl = lamp.getCameraUrl();
        long startTime = System.currentTimeMillis();

        // 使用CompletableFuture异步处理视频帧
        CompletableFuture<String> captureFuture = CompletableFuture.supplyAsync(
                () -> videoFrameHandler.captureFrame(cameraUrl),
                videoTaskExecutor
        );

        // 等待截取完成并压缩
        String frameData = captureFuture
                .thenApply(videoFrameHandler::compressFrame)
                .join();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("视频帧处理完成: lampId={}, 耗时={}ms", lampId, elapsed);

        if (frameData == null) {
            return null;
        }

        VideoFrameDTO dto = new VideoFrameDTO();
        dto.setLampId(lampId);
        dto.setFrameData(frameData);
        dto.setTimestamp(System.currentTimeMillis());
        return dto;
    }

    @Override
    public String getStreamUrl(Long lampId) {
        Lamp lamp = lampMapper.selectById(lampId);
        if (lamp == null) {
            log.warn("灯杆不存在: lampId={}", lampId);
            return null;
        }
        return lamp.getCameraUrl();
    }
}
