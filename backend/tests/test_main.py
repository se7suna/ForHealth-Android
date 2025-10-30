# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())# 获取 backend 目录的绝对路径
sys.path.insert(0, backend_path)

import pytest
from app.main import app
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_root():
    """测试根路径"""
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.get("/")
        assert response.status_code == 200
        assert "message" in response.json()


@pytest.mark.asyncio
async def test_health_check():
    """测试健康检查"""
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
