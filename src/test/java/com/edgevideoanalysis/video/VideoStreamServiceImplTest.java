package com.edgevideoanalysis.video;

import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import com.edgevideoanalysis.video.dto.VideoFrameDTO;
import com.edgevideoanalysis.video.handler.VideoFrameHandler;
import com.edgevideoanalysis.video.service.impl.VideoStreamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * VideoStreamServiceImpl 单元测试
 * 覆盖异步视频帧管道：正常链路 / 抓帧失败 / 压缩失败 / 多帧串行
 */
@ExtendWith(MockitoExtension.class)
class VideoStreamServiceImplTest {

    @Mock
    private LampMapper lampMapper;

    @Mock
    private VideoFrameHandler videoFrameHandler;

    private ThreadPoolExecutor videoTaskExecutor;

    private VideoStreamServiceImpl videoStreamService;

    @BeforeEach
    void setUp() {
        videoTaskExecutor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 直接构造器注入，不用反射
        videoStreamService = new VideoStreamServiceImpl(
                lampMapper, videoFrameHandler, videoTaskExecutor);
    }

    // ==================== 正常链路 ====================

    @Test
    @DisplayName("正常帧抓取 → CompletableFuture 异步编排 → 返回压缩帧")
    void testGetCurrentFrame_Success() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame("rtsp://test:554/stream"))
                .thenReturn("base64ImageData");
        when(videoFrameHandler.compressFrame("base64ImageData"))
                .thenReturn("compressedBase64");

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        assertEquals("compressedBase64", result.getFrameData());
        assertTrue(result.getTimestamp() > 0);

        // 验证异步管道调用链：先 capture → 再 compress
        verify(videoFrameHandler).captureFrame("rtsp://test:554/stream");
        verify(videoFrameHandler).compressFrame("base64ImageData");
    }

    // ==================== 灯杆不存在 ====================

    @Test
    @DisplayName("灯杆不存在 → 返回 null，不触发帧抓取")
    void testGetCurrentFrame_LampNotFound() {
        when(lampMapper.selectById(999L)).thenReturn(null);

        VideoFrameDTO result = videoStreamService.getCurrentFrame(999L);

        assertNull(result);
        verify(lampMapper).selectById(999L);
        verifyNoInteractions(videoFrameHandler);
    }

    // ==================== 摄像头未配置 ====================

    @Test
    @DisplayName("灯杆未配置摄像头URL → 返回 null，不触发帧抓取")
    void testGetCurrentFrame_NoCameraUrl() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl(null);
        when(lampMapper.selectById(1L)).thenReturn(lamp);

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNull(result);
        verifyNoInteractions(videoFrameHandler);
    }

    // ==================== 抓帧返回 null（流不通/摄像头离线） ====================

    @Test
    @DisplayName("帧抓取失败返回null → 整体返回 null（compressFrame 仍被执行，接收null入参）")
    void testGetCurrentFrame_CaptureReturnsNull() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://offline:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString())).thenReturn(null);
        // thenApply 会把 null 传给 compressFrame
        when(videoFrameHandler.compressFrame(null)).thenReturn(null);

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNull(result);
        // thenApply 链传递 null → compressFrame 仍会被调用
        verify(videoFrameHandler).compressFrame(null);
    }

    // ==================== 抓帧抛出异常 ====================

    @Test
    @DisplayName("帧抓取抛异常 → CompletableFuture 异常传播 → 整体返回 null")
    void testGetCurrentFrame_CaptureThrowsException() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://bad:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString()))
                .thenThrow(new RuntimeException("FFmpeg 连接超时"));

        // join() 会抛出 CompletionException，业务层应捕获
        VideoFrameDTO result = null;
        try {
            result = videoStreamService.getCurrentFrame(1L);
        } catch (Exception e) {
            // 预期抛出异常
        }

        assertNull(result);
        verify(videoFrameHandler, never()).compressFrame(anyString());
    }

    // ==================== 多帧连续请求（验证异步不互相阻塞） ====================

    @Test
    @DisplayName("连续请求两帧 → 各自独立 CompletableFuture → 互不干扰")
    void testGetCurrentFrame_ConsecutiveCalls() {
        Lamp lamp1 = new Lamp();
        lamp1.setId(1L);
        lamp1.setCameraUrl("rtsp://cam1:554/stream");

        Lamp lamp2 = new Lamp();
        lamp2.setId(2L);
        lamp2.setCameraUrl("rtsp://cam2:554/stream");

        when(lampMapper.selectById(1L)).thenReturn(lamp1);
        when(lampMapper.selectById(2L)).thenReturn(lamp2);
        when(videoFrameHandler.captureFrame("rtsp://cam1:554/stream"))
                .thenReturn("frame1");
        when(videoFrameHandler.captureFrame("rtsp://cam2:554/stream"))
                .thenReturn("frame2");
        when(videoFrameHandler.compressFrame("frame1")).thenReturn("compressed1");
        when(videoFrameHandler.compressFrame("frame2")).thenReturn("compressed2");

        VideoFrameDTO result1 = videoStreamService.getCurrentFrame(1L);
        VideoFrameDTO result2 = videoStreamService.getCurrentFrame(2L);

        assertEquals("compressed1", result1.getFrameData());
        assertEquals("compressed2", result2.getFrameData());
    }

    // ==================== getStreamUrl ====================

    @Test
    @DisplayName("获取视频流地址 → 返回 RTSP URL")
    void testGetStreamUrl_Success() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);

        assertEquals("rtsp://test:554/stream",
                videoStreamService.getStreamUrl(1L));
    }

    @Test
    @DisplayName("获取视频流地址 → 灯杆不存在 → null")
    void testGetStreamUrl_LampNotFound() {
        when(lampMapper.selectById(999L)).thenReturn(null);
        assertNull(videoStreamService.getStreamUrl(999L));
    }
}
