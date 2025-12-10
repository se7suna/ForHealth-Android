"""
Qwen 多模态模型调用封装

参考根目录下的 `qwen_bailian_api.py`，在后端环境中提供统一的调用接口。

默认使用 qwen3-vl-flash 模型（最新的轻量级多模态模型，响应更快）。
"""

from __future__ import annotations

import os
import base64
from typing import Optional

from openai import OpenAI

try:
    # 可选地从外部配置中读取 QWEN_API_KEY（如果存在）
    from app import config_external_api  # type: ignore
except Exception:  # pragma: no cover - 导入失败时忽略，由环境变量兜底
    config_external_api = None  # type: ignore


def _get_api_key() -> str:
    """
    获取百炼平台 API Key。

    优先顺序：
    1. 显式传入的 api_key（由上层调用决定）
    2. 环境变量 DASHSCOPE_API_KEY
    3. 环境变量 QWEN_API_KEY
    4. 配置文件 app.config_external_api.QWEN_API_KEY（如果已设置且非占位）
    """
    api_key = os.getenv("DASHSCOPE_API_KEY") or os.getenv("QWEN_API_KEY")

    # 尝试从配置文件中读取（如果导入成功且配置了有效值）
    if not api_key and config_external_api is not None:
        candidate = getattr(config_external_api, "QWEN_API_KEY", None)
        if candidate and isinstance(candidate, str) and candidate not in ("", "your-qwen-api-key-here"):
            api_key = candidate

    if not api_key:
        raise ValueError(
            "未配置百炼平台 API Key，请在环境变量 DASHSCOPE_API_KEY 或 QWEN_API_KEY 中配置，"
            "或在 app.config_external_api.QWEN_API_KEY 中填写你的密钥"
        )
    return api_key


def call_qwen_vl_with_url(
    image_url: Optional[str],
    prompt: str,
    api_key: Optional[str] = None,
    model: str = "qwen3-vl-flash",
) -> str:
    """
    使用图片 URL 调用 Qwen-VL 模型，返回模型文本回答。
    image_url 可选，若为 None 则仅发送文本。
    """
    if api_key is None:
        api_key = _get_api_key()

    client = OpenAI(
        api_key=api_key,
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    )

    # 构建 content 列表：如果有图片则包含图片，否则只包含文本
    content_list = []
    if image_url:
        content_list.append({"type": "image_url", "image_url": {"url": image_url}})
    content_list.append({"type": "text", "text": prompt})

    completion = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "user",
                "content": content_list,
            }
        ],
    )

    # 兼容字符串或分段内容
    content = completion.choices[0].message.content
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        # 将多段文本拼接
        texts = [c.get("text", "") for c in content if isinstance(c, dict)]
        return "\n".join(filter(None, texts))
    return str(content)


def call_qwen_vl_with_local_file(
    image_path: str,
    prompt: str,
    api_key: Optional[str] = None,
    model: str = "qwen3-vl-flash",
) -> str:
    """
    使用本地图片文件调用 Qwen-VL 模型。
    """
    # 读取本地图片并转换为 base64 data URL
    with open(image_path, "rb") as image_file:
        image_data = base64.b64encode(image_file.read()).decode("utf-8")

    ext = os.path.splitext(image_path)[1].lower()
    mime_types = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".webp": "image/webp",
        ".bmp": "image/bmp",
    }
    mime_type = mime_types.get(ext, "image/jpeg")

    image_url = f"data:{mime_type};base64,{image_data}"
    return call_qwen_vl_with_url(image_url, prompt, api_key, model)


