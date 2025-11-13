"""
条形码图片识别工具模块
使用 pyzbar 和 opencv-python 从图片中识别条形码

依赖安装:
pip install pyzbar opencv-python pillow numpy
"""

import cv2
from pyzbar import pyzbar
from PIL import Image
import numpy as np
from typing import Optional, Dict
import logging

logger = logging.getLogger(__name__)


def decode_barcode_from_image(image_path: str) -> Optional[Dict[str, str]]:
    """
    从图片中识别条形码，只返回第一个识别到的条形码
    
    Args:
        image_path: 图片路径
    
    Returns:
        识别成功返回:
        {
            'barcode': '条形码数字',
            'type': '条形码类型（如 EAN13, CODE128）'
        }
        识别失败返回: None
    """
    try:
        logger.info(f"开始识别条形码图片: {image_path}")
        
        # 方法1：使用 OpenCV 读取
        try:
            image = cv2.imread(image_path)
            if image is None:
                raise ValueError(f"无法读取图片: {image_path}")
        except Exception as e:
            logger.warning(f"OpenCV 读取失败: {e}，尝试使用 PIL")
            # 方法2：使用 PIL 读取
            try:
                pil_image = Image.open(image_path)
                image = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
            except Exception as e2:
                logger.error(f"PIL 读取也失败: {e2}")
                return None
        
        # 转换为灰度图（提高识别率）
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # 识别条形码
        logger.info("开始识别条形码...")
        barcodes = pyzbar.decode(gray)
        
        if not barcodes:
            logger.info("未识别到条形码，尝试图像增强...")
            
            # 尝试多种图像预处理方法
            methods = [
                ("二值化", cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]),
                ("自适应阈值", cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)),
                ("模糊处理", cv2.GaussianBlur(gray, (5, 5), 0)),
                ("锐化", cv2.filter2D(gray, -1, np.array([[-1,-1,-1], [-1,9,-1], [-1,-1,-1]]))),
            ]
            
            for method_name, processed in methods:
                barcodes = pyzbar.decode(processed)
                if barcodes:
                    logger.info(f"使用 {method_name} 成功识别")
                    break
        
        if not barcodes:
            logger.warning("所有方法都未能识别到条形码")
            return None
        
        # 返回第一个识别到的条形码
        barcode = barcodes[0]
        barcode_data = barcode.data.decode('utf-8')
        barcode_type = barcode.type
        
        logger.info(f"成功识别条形码: {barcode_data} (类型: {barcode_type})")
        
        return {
            'barcode': barcode_data,
            'type': barcode_type
        }
    
    except Exception as e:
        logger.error(f"识别条形码时发生错误: {e}", exc_info=True)
        return None


def validate_barcode(barcode: str) -> bool:
    """
    简单验证条形码格式
    
    Args:
        barcode: 条形码字符串
    
    Returns:
        是否为有效的条形码格式
    """
    if not barcode:
        return False
    
    # 常见条形码长度
    valid_lengths = [8, 12, 13, 14]  # EAN-8, UPC-A, EAN-13, ITF-14
    
    # 基本验证：是否为纯数字且长度合法
    if barcode.isdigit() and len(barcode) in valid_lengths:
        return True
    
    # CODE128 等可能包含字母
    if len(barcode) >= 4:
        return True
    
    return False

