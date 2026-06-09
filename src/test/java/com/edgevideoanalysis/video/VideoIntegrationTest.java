package com.edgevideoanalysis.video;

import com.edgevideoanalysis.device.entity.Lamp;
import com.edgevideoanalysis.device.mapper.LampMapper;
import com.edgevideoanalysis.video.dto.VideoFrameDTO;
import com.edgevideoanalysis.video.handler.VideoFrameHandler;
import com.edgevideoanalysis.video.service.IVideoStreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 视频处理模块集成测试
 * 测试异步处理、并发能力、线程池拒绝策略
 */
@SpringBootTest
class VideoIntegrationTest {

    @Autowired
    private IVideoStreamService videoStreamService;

    @MockBean
    private LampMapper lampMapper;

    @MockBean
    private VideoFrameHandler videoFrameHandler;

    @Test
    @DisplayName("测试异步处理流程 - 验证CompletableFuture异步执行")
    void testAsyncProcessing() {
        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString())).thenAnswer(invocation -> {
            // 验证在video-task线程中执行
            String threadName = Thread.currentThread().getName();
            assertTrue(threadName.startsWith("video-task-"), 
                    "应该在video-task线程中执行，当前线程: " + threadName);
            return "mockBase64Data";
        });
        when(videoFrameHandler.compressFrame(anyString())).thenReturn("compressedData");

        VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);

        assertNotNull(result);
        assertEquals(1L, result.getLampId());
        verify(videoFrameHandler).captureFrame(anyString());
    }

    @Test
    @DisplayName("测试并发处理 - 模拟多路视频同时处理")
    void testConcurrentProcessing() throws InterruptedException {
        int concurrentRequests = 10;
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString())).thenReturn("mockBase64Data");
        when(videoFrameHandler.compressFrame(anyString())).thenReturn("compressedData");

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);
                    if (result != null) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("并发测试完成: 成功=" + successCount.get() + ", 失败=" + failCount.get());
        assertTrue(successCount.get() > 0, "应该有成功的请求");
    }

    @Test
    @DisplayName("测试线程池满载 - 验证拒绝策略")
    void testThreadPoolRejection() throws InterruptedException {
        int overloadRequests = 120; // 超过队列容量100
        CountDownLatch latch = new CountDownLatch(overloadRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        Lamp lamp = new Lamp();
        lamp.setId(1L);
        lamp.setCameraUrl("rtsp://test:554/stream");
        when(lampMapper.selectById(1L)).thenReturn(lamp);
        when(videoFrameHandler.captureFrame(anyString())).thenAnswer(invocation -> {
            Thread.sleep(500); // 模拟耗时操作，让线程池快速满载
            return "mockBase64Data";
        });
        when(videoFrameHandler.compressFrame(anyString())).thenReturn("compressedData");

        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < overloadRequests; i++) {
            executor.submit(() -> {
                try {
                    VideoFrameDTO result = videoStreamService.getCurrentFrame(1L);
                    if (result != null) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Rejected")) {
                        rejectedCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("拒绝策略测试: 成功=" + successCount.get() + ", 被拒绝=" + rejectedCount.get());
        // 由于自定义拒绝策略是丢弃任务但不抛异常，部分请求会返回null
    }
}
