-- 创建数据库
CREATE DATABASE IF NOT EXISTS edge_video_analysis DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE edge_video_analysis;

-- 1. 灯杆设备表
CREATE TABLE IF NOT EXISTS t_lamp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_code VARCHAR(50) NOT NULL COMMENT '灯杆编号',
    lamp_name VARCHAR(100) NOT NULL COMMENT '灯杆名称',
    location VARCHAR(255) COMMENT '位置描述',
    camera_url VARCHAR(500) COMMENT '视频流地址',
    led_status INT DEFAULT 0 COMMENT 'LED状态 0-关 1-开',
    online_status INT DEFAULT 0 COMMENT '在线状态 0-离线 1-在线',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_lamp_code (lamp_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灯杆设备表';

-- 2. 传感器数据表
CREATE TABLE IF NOT EXISTS t_sensor_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_id BIGINT NOT NULL COMMENT '关联灯杆ID',
    temperature DOUBLE COMMENT '温度',
    humidity DOUBLE COMMENT '湿度',
    illumination DOUBLE COMMENT '光照强度',
    voltage DOUBLE COMMENT '电压',
    current DOUBLE COMMENT '电流',
    capture_time DATETIME COMMENT '采集时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sensor_lamp_time (lamp_id, capture_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器数据表';

-- 3. 报警规则表
CREATE TABLE IF NOT EXISTS t_alarm_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_id BIGINT NOT NULL COMMENT '关联灯杆ID',
    sensor_type VARCHAR(50) NOT NULL COMMENT '传感器类型',
    upper_limit DOUBLE COMMENT '上限',
    lower_limit DOUBLE COMMENT '下限',
    enabled INT DEFAULT 1 COMMENT '是否启用 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警规则表';

-- 4. 报警记录表
CREATE TABLE IF NOT EXISTS t_alarm_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_id BIGINT NOT NULL COMMENT '关联灯杆ID',
    sensor_type VARCHAR(50) NOT NULL COMMENT '传感器类型',
    sensor_value DOUBLE COMMENT '触发报警的值',
    alarm_level INT COMMENT '报警级别',
    alarm_message VARCHAR(500) COMMENT '报警描述',
    alarm_time DATETIME COMMENT '报警时间',
    handled INT DEFAULT 0 COMMENT '是否已处理 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_alarm_lamp_time (lamp_id, alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警记录表';

-- 5. 推理记录表
CREATE TABLE IF NOT EXISTS t_inference_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_id BIGINT NOT NULL COMMENT '关联灯杆ID',
    original_image TEXT COMMENT '原始图片Base64',
    processed_image TEXT COMMENT '框选后图片Base64',
    person_count INT DEFAULT 0 COMMENT '识别到的人数',
    inference_results TEXT COMMENT 'JSON推理结果数组',
    inference_time DATETIME COMMENT '推理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_inference_lamp_time (lamp_id, inference_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推理记录表';

-- 6. 控制指令表
CREATE TABLE IF NOT EXISTS t_control_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    lamp_id BIGINT NOT NULL COMMENT '关联灯杆ID',
    command_type VARCHAR(50) NOT NULL COMMENT '命令类型',
    command_status INT DEFAULT 0 COMMENT '状态 0-待执行 1-执行中 2-成功 3-失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    execute_time DATETIME COMMENT '执行时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='控制指令表';
