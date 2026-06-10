package com.edgevideoanalysis.websocket;

import com.edgevideoanalysis.websocket.service.DeviceHeartbeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DeviceHeartbeatServiceTest {

    @Autowired
    private DeviceHeartbeatService deviceHeartbeatService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testInitHeartbeat() {
        String lampId = "test_1";
        
        // Cleanup first
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Verify
        String version = stringRedisTemplate.opsForValue().get("device:version:" + lampId);
        String status = stringRedisTemplate.opsForValue().get("device:status:" + lampId);
        
        assertEquals("0", version);
        assertEquals("online", status);
        
        cleanup(lampId);
    }

    @Test
    public void testCheckHeartbeat_Valid() {
        String lampId = "test_2";
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Check heartbeat with version 1 (current is 0, so next should be 1)
        assertTrue(deviceHeartbeatService.checkHeartbeat(lampId, 1));
        
        cleanup(lampId);
    }

    @Test
    public void testCheckHeartbeat_Invalid() {
        String lampId = "test_3";
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Check with wrong version (should be 1, but we send 5)
        assertFalse(deviceHeartbeatService.checkHeartbeat(lampId, 5));
        
        cleanup(lampId);
    }

    @Test
    public void testCheckHeartbeat_NotExists() {
        String lampId = "test_nonexist";
        cleanup(lampId);
        
        // Check without initializing
        assertFalse(deviceHeartbeatService.checkHeartbeat(lampId, 1));
    }

    @Test
    public void testUpdateHeartbeat() {
        String lampId = "test_4";
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Update heartbeat
        int newVersion = deviceHeartbeatService.updateHeartbeat(lampId);
        assertEquals(1, newVersion);
        
        // Update again
        newVersion = deviceHeartbeatService.updateHeartbeat(lampId);
        assertEquals(2, newVersion);
        
        // Verify Redis
        String version = stringRedisTemplate.opsForValue().get("device:version:" + lampId);
        assertEquals("2", version);
        
        cleanup(lampId);
    }

    @Test
    public void testHandleDisconnect() {
        String lampId = "test_5";
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Handle disconnect
        deviceHeartbeatService.handleDisconnect(lampId);
        
        // Verify status changed to offline
        String status = stringRedisTemplate.opsForValue().get("device:status:" + lampId);
        assertEquals("offline", status);
        
        cleanup(lampId);
    }

    @Test
    public void testGetDeviceStatus() {
        String lampId = "test_6";
        cleanup(lampId);
        
        // Initialize
        deviceHeartbeatService.initHeartbeat(lampId);
        
        // Check status
        assertEquals("online", deviceHeartbeatService.getDeviceStatus(lampId));
        
        // Disconnect
        deviceHeartbeatService.handleDisconnect(lampId);
        assertEquals("offline", deviceHeartbeatService.getDeviceStatus(lampId));
        
        cleanup(lampId);
    }

    private void cleanup(String lampId) {
        stringRedisTemplate.delete("device:version:" + lampId);
        stringRedisTemplate.delete("device:status:" + lampId);
    }
}
