# 智慧城市边缘视频分析平台

基于 Spring Boot + YOLOv8 的智能灯杆边缘视频分析系统，实现设备管理、传感器监控、行人检测、报警联动与远程控制。

## ✨ 核心功能

- **设备管理** - 灯杆 CRUD、在线状态监控、视频流配置
- **传感器监测** - 温湿度、光照、电压电流实时采集与历史曲线查询
- **AI 行人检测** - YOLOv8 实时识别行人，支持截图与结果回放
- **智能报警** - 传感器阈值设置，超限自动告警与推送
- **远程控制** - WebSocket/HTTP 双通道 LED 远程开关
- **数据可视化** - 传感器曲线、报警记录、推理记录多维查询

## 🚀 性能优化亮点

| 优化项 | 优化前 | 优化后 |
|--------|--------|--------|
| 单帧识别耗时 | ~750ms | **~190ms** |
| 历史查询响应 | ~300ms | **~90ms** |
| 设备状态同步 | 不稳定 | **99%+ 准确率** |

**技术方案**：
- 自定义线程池 + CompletableFuture 异步处理视频帧
- WebSocket + Redis 版本号心跳机制保障设备状态一致性
- MySQL 复合索引 + Redis 缓存加速历史数据查询

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7 |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| AI 推理 | YOLOv8 (Python/Flask) |
| 视频处理 | JavaCV / FFmpeg |
| 实时通信 | WebSocket |
| 容器化 | Docker + Docker Compose |

## 📦 快速开始

### Docker Compose 部署（推荐）

```bash
# 克隆项目
git clone <repository-url>
cd EdgeVideoAnalysis

# 一键启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

服务启动后访问：
- 后端 API: `http://localhost:8080`
- YOLOv8 推理服务: `http://localhost:5000`

### 本地开发运行

```bash
# 1. 启动 MySQL 和 Redis
# 2. 启动 YOLOv8 服务
cd yolov8-service
pip install -r requirements.txt
python app.py

# 3. 启动后端
mvn clean package
java -jar target/edge-video-analysis-1.0.0.jar
```

## 📁 项目结构

```
EdgeVideoAnalysis/
├── src/main/java/com/edgevideoanalysis/
│   ├── device/          # 设备管理
│   ├── sensor/          # 传感器数据
│   ├── alarm/           # 报警规则与记录
│   ├── video/           # 视频流处理（线程池+异步）
│   ├── ai/              # YOLOv8 推理集成
│   ├── websocket/       # 设备心跳与状态推送
│   ├── control/         # 远程控制
│   └── common/          # 公共配置、异常处理、统一响应
├── yolov8-service/      # YOLOv8 推理服务（Python/Flask）
├── docker-compose.yml
└── pom.xml
```

## 📖 API 文档

主要接口示例：

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 设备 | GET | `/api/lamp/list` | 获取灯杆列表 |
| 传感器 | POST | `/api/sensor/report` | 上报传感器数据 |
| 传感器 | GET | `/api/sensor/curve` | 获取曲线数据 |
| 视频 | GET | `/api/video/frame/{lampId}` | 获取当前帧 |
| AI | POST | `/api/ai/inference` | 执行行人检测 |
| 控制 | POST | `/api/control/led` | 控制 LED 开关 |
| WebSocket | ws | `/ws/device` | 设备心跳连接 |

详细接口定义见 `API接口文档.md`。

## 🏆 项目成果

- 荣获 **全国大学生物联网设计大赛湖南赛区一等奖**
- 稳定支持 4+ 路摄像头并发处理
- 完整实现设备接入、AI 推理调度、历史数据可视化五大功能模块

## 📄 许可证

MIT License

## 👥 作者

CoderDongHuang
