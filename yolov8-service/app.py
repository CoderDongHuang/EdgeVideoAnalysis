"""
YOLOv8推理服务
提供HTTP接口供后端调用
"""
from flask import Flask, request, jsonify
from ultralytics import YOLO
import base64
import io
import numpy as np
from PIL import Image
import time
import os

app = Flask(__name__)

# 加载模型（从环境变量获取模型路径，默认使用yolov8n.pt）
MODEL_PATH = os.getenv('MODEL_PATH', 'yolov8n.pt')
model = YOLO(MODEL_PATH)

# 置信度阈值
CONFIDENCE_THRESHOLD = float(os.getenv('CONFIDENCE_THRESHOLD', '0.5'))


@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({'status': 'ok', 'model': MODEL_PATH})


@app.route('/inference', methods=['POST'])
def inference():
    """
    推理接口
    请求体: {"image": "base64编码的图片数据"}
    响应: {"person_count": 2, "results": [...], "inference_time": 150}
    """
    try:
        # 获取请求数据
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({'error': '缺少image参数'}), 400

        # 解码base64图片
        image_data = base64.b64decode(data['image'])
        image = Image.open(io.BytesIO(image_data))

        # 转换为numpy数组
        image_np = np.array(image)

        # 执行推理
        start_time = time.time()
        results = model(image_np, conf=CONFIDENCE_THRESHOLD)
        inference_time = int((time.time() - start_time) * 1000)

        # 解析结果
        detections = []
        person_count = 0

        for result in results:
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                confidence = float(box.conf[0])
                class_id = int(box.cls[0])
                class_name = result.names[class_id]

                detection = {
                    'class': class_name,
                    'class_id': class_id,
                    'confidence': round(confidence, 3),
                    'bbox': {
                        'x1': round(x1, 2),
                        'y1': round(y1, 2),
                        'x2': round(x2, 2),
                        'y2': round(y2, 2)
                    }
                }
                detections.append(detection)

                # 统计人数（COCO数据集中person的class_id为0）
                if class_id == 0:
                    person_count += 1

        return jsonify({
            'person_count': person_count,
            'results': detections,
            'inference_time': inference_time
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
