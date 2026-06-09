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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * VideoStreamServiceImpl 单元测试
 * 测试异步处理流程与服务层逻辑
 */
@ExtendWith(MockitoExtension.class)
class VideoStreamServiceImplTest {

    @Mock
    private LampMapper lampMapper;

    @Mock
    private VideoFrameHandler videoFrameHandler;

    @InjectMocks
    private VideoStreamServiceImpl videoStreamService;

    private ThreadPoolExecutor videoTaskExecutor;

    @BeforeEach
    void setUp() {
        // 创建测试用线程池
        videoTaskExecutor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10)
        );
        // 通过反射注入（因为@InjectMocks无法注入@Bean创建的线程池）
        try {
            var field = VideoStreamServiceImpl.class.getDeclaredField("videoTaskExecutor");
            field.setAccessible(true);
            field.set(videoStreamService, videoTaskExecutor);
        } catch (Exception e) {
            fail("注入线程池失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试获取当前帧 - 灯杆不存在")
    void testGetCurrentFrame_LampNotFound() {
        when(lampMapper.selectById(999L)).thenReturn(null);

        VideoFrameDTO result = videoStreamService.getCurrentFrame(999L);

        assertNull(result);
        verify(lampMapper).selectById(999L);
        verifyNoInteractions(videoFrameHandler);
    }

    @Test
    @DisplayName("测试获取当前帧 - 未配置摄像头URL")
    void testGetCurrentFrame_NoCameraUrl() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl(null);
        when(lampMapper.selectById(1L)).thenReturn(lamp);

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNull(result);
        verify(lampMapper).selectById(1L);
        verifyNoInteractions(videoFrameHandler);
    }

    @Test
    @DisplayName("测试获取当前帧 - 正常流程")
    void testGetCurrentFrame_Success() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString())).thenReturn("base64data");
        when(videoFrameHandler.compressFrame(anyString())).thenReturn("compressedBase64");

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        assertEquals("compressedBase64", result.getFrameData());
        assertNotNull(result.getTimestamp());

        verify(videoFrameHandler).captureFrame("rtsp://test:554/stream");
        verify(videoFrameHandler).compressFrame("base64data");
    }

    @Test
    @DisplayName("测试获取视频流地址 - 正常")
    void testGetStreamUrl_Success() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);

        String result = videoStreamService.getStreamUrl(1L);

        assertEquals("rtsp://test:554/stream", result);
    }

    @Test
    @DisplayName("测试获取视频流地址 - 灯杆不存在")
    void testGetStreamUrl_LampNotFound() {
        when(lampMapper.selectById(999L)).thenReturn(null);

        String result = videoStreamService.getStreamUrl(999L);

        assertNull(result);
    }
}
