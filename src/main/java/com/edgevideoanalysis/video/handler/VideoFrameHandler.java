package com.edgevideoanalysis.video.handler;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 视频帧处理器
 * 封装FFmpeg调用逻辑，实现帧截取与分块压缩
 */
@Slf4j
@Component
public class VideoFrameHandler {

    /**
     * 截取视频流当前帧
     * @param cameraUrl 摄像头RTSP/HTTP流地址
     * @return Base64编码的帧图片数据
     */
    public String captureFrame(String cameraUrl) {
        FFmpegFrameGrabber grabber = null;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        
        try {
            grabber = new FFmpegFrameGrabber(cameraUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.start();
            
            // 抓取一帧作为当前帧
            Frame frame = grabber.grabImage();
            if (frame == null) {
                log.warn("无法从视频流获取帧: {}", cameraUrl);
                return null;
            }
            
            // 将Frame转换为BufferedImage
            BufferedImage bufferedImage = converter.convert(frame);
            if (bufferedImage == null) {
                log.warn("帧转换BufferedImage失败: {}", cameraUrl);
                return null;
            }
            
            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            return Base64.getEncoder().encodeToString(imageBytes);
            
        } catch (Exception e) {
            log.error("截取视频帧失败: {}", cameraUrl, e);
            return null;
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                log.error("释放FFmpeg资源失败", e);
            }
        }
    }

    /**
     * 分块压缩Base64帧数据
     * 核心优化点：将大Base64字符串分块传输，减少单次传输压力
     * @param frameData Base64编码的帧数据
     * @return 压缩后的Base64数据（简化版，实际可按需分块）
     */
    public String compressFrame(String frameData) {
        if (frameData == null || frameData.isEmpty()) {
            return frameData;
        }
        
        try {
            // 解码Base64
            byte[] decodedBytes = Base64.getDecoder().decode(frameData);
            
            // 如果数据较小，直接返回原数据
            if (decodedBytes.length < 1024 * 100) { // 小于100KB不压缩
                return frameData;
            }
            
            // 重新编码为更紧凑的格式（JPEG质量压缩）
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(decodedBytes));
            if (image == null) {
                return frameData;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            
            return Base64.getEncoder().encodeToString(baos.toByteArray());
            
        } catch (Exception e) {
            log.error("压缩帧数据失败", e);
            return frameData;
        }
    }
}
