# 智慧城市边缘视频分析平台 - API接口文档

## 统一响应格式

所有接口均返回以下格式的JSON数据：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code`: 状态码，200表示成功，其他表示错误
- `message`: 提示信息
- `data`: 响应数据

---

## 一、设备管理模块 (Device)

### 1.1 获取灯杆列表
**接口**: `GET /api/lamp/list`

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "lampCode": "lamp01",
      "lampName": "灯杆01",
      "location": "南门入口",
      "cameraUrl": "rtsp://192.168.1.100/stream",
      "ledStatus": 1,
      "onlineStatus": 1,
      "createTime": "2024-01-01 10:00:00"
    }
  ]
}
```

### 1.2 获取灯杆详情
**接口**: `GET /api/lamp/detail/{id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 灯杆ID |

**响应示例**: 同列表项

### 1.3 获取灯杆状态
**接口**: `GET /api/lamp/{id}/status`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 灯杆ID |

### 1.4 创建灯杆
**接口**: `POST /api/lamp`

**请求体**:
```json
{
  "lampCode": "lamp04",
  "lampName": "灯杆04",
  "location": "北门",
  "cameraUrl": "rtsp://192.168.1.104/stream"
}
```

### 1.5 更新灯杆
**接口**: `PUT /api/lamp/{id}`

### 1.6 删除灯杆
**接口**: `DELETE /api/lamp/{id}`

---

## 二、传感器模块 (Sensor)

### 2.1 上报传感器数据
**接口**: `POST /api/sensor/report`

**请求体**:
```json
{
  "lampId": 1,
  "temperature": 23.5,
  "humidity": 65.0,
  "illumination": 300.0,
  "voltage": 220.0,
  "current": 2.5,
  "captureTime": "2024-01-01 10:30:00"
}
```

### 2.2 获取最新传感器数据
**接口**: `GET /api/sensor/{lampId}/latest`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 100,
    "lampId": 1,
    "temperature": 23.5,
    "humidity": 65.0,
    "illumination": 300.0,
    "voltage": 220.0,
    "current": 2.5,
    "captureTime": "2024-01-01 10:30:00"
  }
}
```

### 2.3 历史数据查询
**接口**: `GET /api/sensor/history`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lampId | Long | 是 | 灯杆ID |
| startTime | String | 是 | 开始时间 (yyyy-MM-dd HH:mm:ss) |
| endTime | String | 是 | 结束时间 |

### 2.4 曲线数据
**接口**: `GET /api/sensor/curve`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lampId | Long | 是 | 灯杆ID |
| sensorType | String | 是 | 传感器类型 (temperature/humidity/illumination/voltage/current) |
| startTime | String | 是 | 开始时间 |
| endTime | String | 是 | 结束时间 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"time": "2024-01-01 10:00:00", "value": 23.1},
    {"time": "2024-01-01 10:05:00", "value": 23.3},
    {"time": "2024-01-01 10:10:00", "value": 23.5}
  ]
}
```

---

## 三、报警模块 (Alarm)

### 3.1 设置报警规则
**接口**: `POST /api/alarm/rule`

**请求体**:
```json
{
  "lampId": 1,
  "sensorType": "temperature",
  "upperLimit": 35.0,
  "lowerLimit": -10.0,
  "enabled": 1
}
```

### 3.2 获取报警规则
**接口**: `GET /api/alarm/rule/{lampId}`

### 3.3 查询报警记录
**接口**: `GET /api/alarm/records`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lampId | Long | 否 | 灯杆ID |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |
| handled | Integer | 否 | 是否已处理 (0-未处理, 1-已处理) |

### 3.4 报警统计
**接口**: `GET /api/alarm/stats`

---

## 四、视频处理模块 (Video)

### 4.1 获取当前帧
**接口**: `GET /api/video/frame/{lampId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "lampId": 1,
    "frameData": "base64编码的图片数据",
    "timestamp": "2024-01-01 10:30:00"
  }
}
```

### 4.2 获取视频流地址
**接口**: `GET /api/video/stream/{lampId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "rtsp://192.168.1.100/stream"
}
```

---

## 五、AI推理模块 (AI)

### 5.1 执行AI推理
**接口**: `POST /api/ai/inference`

**请求体**:
```json
{
  "image": "base64编码的图片数据",
  "lampId": 1
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "lampId": 1,
    "originalImage": "base64...",
    "processedImage": "base64...",
    "personCount": 2,
    "inferenceResults": "[{\"class\":\"person\",\"confidence\":0.95}]",
    "inferenceTime": "2024-01-01 10:30:00"
  }
}
```

### 5.2 查询推理记录
**接口**: `GET /api/ai/records`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lampId | Long | 否 | 灯杆ID |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |

### 5.3 获取推理详情
**接口**: `GET /api/ai/records/{id}`

---

## 六、设备控制模块 (Control)

### 6.1 控制LED灯
**接口**: `POST /api/control/led`

**请求体**:
```json
{
  "lampId": 1,
  "commandType": "led_on"
}
```

commandType可选值: `led_on` (开启), `led_off` (关闭)

### 6.2 查询指令状态
**接口**: `GET /api/control/status/{commandId}`

---

## WebSocket接口

### 连接地址
```
ws://{host}:{port}/ws/device
```

### 心跳机制
客户端需定期发送心跳消息：
```json
{
  "type": "heartbeat",
  "lampId": 1,
  "version": 1
}
```

### 服务端推送
服务端会主动推送以下消息：
- 设备状态变更通知
- 报警信息
- 传感器最新数据
