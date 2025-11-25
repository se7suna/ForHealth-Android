# -*- coding: utf-8 -*-
"""
百炼平台 - Qwen多模态模型API调用
"""

import os
import base64
from openai import OpenAI


def call_qwen_vl_with_url(image_url, prompt, api_key=None, model="qwen-vl-plus"):
    """
    使用图片URL调用Qwen-VL模型
    
    参数:
        image_url: 图片的URL地址
        prompt: 对图片的提问
        api_key: 百炼平台API密钥
        model: 模型名称 (qwen-vl-plus / qwen-vl-max / qwen-vl-ocr)
    
    返回:
        模型的响应结果
    """
    # 获取API密钥
    if api_key is None:
        api_key = os.getenv('DASHSCOPE_API_KEY')
    
    if not api_key:
        raise ValueError("请设置API密钥")
    
    # 创建客户端
    client = OpenAI(
        api_key=api_key,
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    )
    
    # 调用API
    completion = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": image_url}},
                    {"type": "text", "text": prompt}
                ]
            }
        ]
    )
    
    return completion.choices[0].message.content


def call_qwen_vl_with_local_file(image_path, prompt, api_key=None, model="qwen-vl-plus"):
    """
    使用本地图片文件调用Qwen-VL模型
    
    参数:
        image_path: 本地图片文件路径
        prompt: 对图片的提问
        api_key: 百炼平台API密钥
        model: 模型名称 (qwen-vl-plus / qwen-vl-max / qwen-vl-ocr)
    
    返回:
        模型的响应结果
    """
    # 读取本地图片并转换为base64
    with open(image_path, 'rb') as image_file:
        image_data = base64.b64encode(image_file.read()).decode('utf-8')
    
    # 根据文件扩展名确定MIME类型
    ext = os.path.splitext(image_path)[1].lower()
    mime_types = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif',
        '.webp': 'image/webp',
        '.bmp': 'image/bmp'
    }
    mime_type = mime_types.get(ext, 'image/jpeg')
    
    # 转换为data URL
    image_url = f"data:{mime_type};base64,{image_data}"
    
    # 使用已有的函数调用API
    return call_qwen_vl_with_url(image_url, prompt, api_key, model)
