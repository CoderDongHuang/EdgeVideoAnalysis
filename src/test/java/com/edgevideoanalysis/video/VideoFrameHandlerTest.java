package com.edgevideoanalysis.video;

import com.edgevideoanalysis.video.handler.VideoFrameHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VideoFrameHandler 单元测试
 * 测试帧截取与压缩功能
 */
@SpringBootTest
class VideoFrameHandlerTest {

    private VideoFrameHandler videoFrameHandler;

    @BeforeEach
    void setUp() {
        videoFrameHandler = new VideoFrameHandler();
    }

    @Test
    @DisplayName("测试压缩空数据")
    void testCompressFrame_NullData() {
        String result = videoFrameHandler.compressFrame(null);
        assertNull(result);
    }

    @Test
    @DisplayName("测试压缩空字符串")
    void testCompressFrame_EmptyData() {
        String result = videoFrameHandler.compressFrame("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("测试压缩小数据（小于100KB直接返回）")
    void testCompressFrame_SmallData() {
        // 创建一个小于100KB的Base64数据
        byte[] smallBytes = new byte[1024]; // 1KB
        String base64 = java.util.Base64.getEncoder().encodeToString(smallBytes);
        
        String result = videoFrameHandler.compressFrame(base64);
        assertEquals(base64, result);
    }
}
