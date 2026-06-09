package com.edgevideoanalysis.video.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 视频帧处理自定义拒绝策略
 * 核心优化点：任务过载时丢弃当前帧，避免线程阻塞，保障系统高可用
 */
@Slf4j
public class VideoFrameRejectHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn("视频处理线程池已满 [核心线程:{}, 活跃线程:{}, 队列大小:{}], 丢弃当前帧任务，避免线程阻塞",
                executor.getCorePoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size());
        // 丢弃当前帧，不阻塞主流程
    }
}
