package com.edgevideoanalysis.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "globalThreadPool")
    public ThreadPoolExecutor globalThreadPool() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4,                          // 核心线程数
                7,                          // 最大线程数
                60L,                        // 空闲线程存活时间
                TimeUnit.SECONDS,           // 时间单位
                new LinkedBlockingQueue<>(100),  // 有界阻塞队列
                new ThreadPoolExecutor.CallerRunsPolicy()  // 自定义拒绝策略：调用者运行
        );

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);

        log.info("全局线程池初始化完成 - 核心线程: 4, 最大线程: 7, 队列容量: 100");
        return executor;
    }
}
