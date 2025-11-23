
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
import pytest_asyncio
from httpx import AsyncClient

# 测试用户凭证
from app.config import settings
TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD
}

# ================== Fixtures：建立可认证的客户端 ==================
@pytest_asyncio.fixture
async def auth_client():
    """创建已认证的客户端"""
    async with AsyncClient(base_url="http://127.0.0.1:8000", timeout=30.0, http2=False) as client:
        # 登录获取 token
        response = await client.post(
            "/api/auth/login",
            json={
                "email": TEST_USER["email"],
                "password": TEST_USER["password"]
            }
        )
        assert response.status_code == 200, f"登录失败: 状态码={response.status_code}, 响应={response.text}"
        token = response.json()["access_token"]
        async with AsyncClient(
            base_url="http://127.0.0.1:8000",
            headers={"Authorization": f"Bearer {token}"},
            timeout=30.0,
            http2=False
        ) as client:
            yield client

# ================== 测试：初始化食物表成功 ==================

# ================== 测试：初始化运动类型成功 ==================
@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,expected_status,expected_success", [
    # 正常情况
    ({"sport_type": "自定义跑步", "describe": "户外跑步", "METs": 8.0}, 200, True),
    # 边界情况：缺少必填字段
    #({"sport_type": "", "describe": "户外跑步", "METs": 8.0}, 422, False),
    # 边界情况：METs为负数
    #({"sport_type": "自定义游泳", "describe": "室内游泳", "METs": -5.0}, 422, False),
])
async def test_create_sports(auth_client,sport_data, expected_status, expected_success):
    """测试创建自定义运动类型 - 正常情况和边界条件"""
    response = None
    try:
        response = await auth_client.post("/api/sports/create-sport", json=sport_data)
        result = response.json()
        assert response.status_code == expected_status
        assert result["success"] == expected_success
    finally:
        if response and result.get("success"):        # 完成测试后删除创建的运动类型
            response = await auth_client.delete(f"/api/sports/delete-sport/{sport_data['sport_type']}")
            assert response.status_code == 200