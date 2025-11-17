import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
import pytest_asyncio
from httpx import AsyncClient

# 测试用户凭证
TEST_USER = {
    "email": "user@example.com",
    "password": "string"
}

# ================== Fixtures ==================

@pytest_asyncio.fixture
async def auth_token():
    """获取认证 token"""
    # 尝试先测试健康检查
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
    token_data = response.json()
    return token_data["access_token"]



@pytest_asyncio.fixture
async def authenticated_client(auth_token):
    """返回已认证的客户端"""
    async with AsyncClient(
        base_url="http://127.0.0.1:8000",
        headers={"Authorization": f"Bearer {auth_token}"},
        timeout=30.0,
        http2=False
    ) as client:
        yield client


# ================== 测试：创建自定义运动类型 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,expected_status,expected_success", [
    # 正常情况
    ({"sport_type": "自定义跑步2", "describe": "户外跑步", "METs": 8.0}, 200, True),
    # 边界情况：缺少必填字段
    #({"sport_type": "", "describe": "户外跑步", "METs": 8.0}, 422, False),
    # 边界情况：METs为负数
    #({"sport_type": "自定义游泳", "describe": "室内游泳", "METs": -5.0}, 422, False),
])
async def test_create_sports(authenticated_client,sport_data, expected_status, expected_success):
    """测试创建自定义运动类型 - 正常情况和边界条件"""
    response = await authenticated_client.post("/api/sports/create-sport", json=sport_data)
        
    assert response.status_code == expected_status # 若报错，响应内容为：response.detail
    result = response.json()
    assert result["success"] == expected_success