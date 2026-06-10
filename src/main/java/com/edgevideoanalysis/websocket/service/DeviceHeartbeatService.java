package com.edgevideoanalysis.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 设备心跳服务
 * 管理设备心跳版本号和在线状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEVICE_VERSION_KEY = "device:version:";
    private static final String DEVICE_STATUS_KEY = "device:status:";
    private static final Duration TTL = Duration.ofMinutes(30);

    /**
     * 初始化心跳版本号
     * @param lampId 灯杆ID
     */
    public void initHeartbeat(String lampId) {
        String versionKey = DEVICE_VERSION_KEY + lampId;
        stringRedisTemplate.opsForValue().set(versionKey, "0", TTL);

        String statusKey = DEVICE_STATUS_KEY + lampId;
        stringRedisTemplate.opsForValue().set(statusKey, "online", TTL);

        log.info("初始化心跳: lampId={}", lampId);
    }

    /**
     * 校验版本号
     * @param lampId 灯杆ID
     * @param version 客户端上报的版本号
     * @return 版本号是否有效
     */
    public boolean checkHeartbeat(String lampId, int version) {
        String versionKey = DEVICE_VERSION_KEY + lampId;
        String currentVersion = stringRedisTemplate.opsForValue().get(versionKey);

        if (currentVersion == null) {
            log.warn("心跳版本号不存在: lampId={}", lampId);
            return false;
        }

        int current = Integer.parseInt(currentVersion);
        if (version != current + 1) {
            log.warn("心跳版本号不匹配: lampId={}, expected={}, got={}",
                    lampId, current + 1, version);
            return false;
        }

        return true;
    }

    /**
     * 更新版本号
     * @param lampId 灯杆ID
     * @return 新的版本号
     */
    public int updateHeartbeat(String lampId) {
        String versionKey = DEVICE_VERSION_KEY + lampId;
        String currentVersion = stringRedisTemplate.opsForValue().get(versionKey);
        int newVersion = currentVersion != null ? Integer.parseInt(currentVersion) + 1 : 1;

        stringRedisTemplate.opsForValue().set(versionKey, String.valueOf(newVersion), TTL);

        // 刷新在线状态TTL
        String statusKey = DEVICE_STATUS_KEY + lampId;
        stringRedisTemplate.expire(statusKey, TTL);

        log.debug("更新心跳: lampId={}, version={}", lampId, newVersion);
        return newVersion;
    }

    /**
     * 处理断线
     * @param lampId 灯杆ID
     */
    public void handleDisconnect(String lampId) {
        String statusKey = DEVICE_STATUS_KEY + lampId;
        stringRedisTemplate.opsForValue().set(statusKey, "offline", TTL);

        log.info("处理设备断线: lampId={}", lampId);
    }

    /**
     * 获取设备在线状态
     * @param lampId 灯杆ID
     * @return 在线状态 (online/offline)
     */
    public String getDeviceStatus(String lampId) {
        String statusKey = DEVICE_STATUS_KEY + lampId;
        return stringRedisTemplate.opsForValue().get(statusKey);
    }
}
