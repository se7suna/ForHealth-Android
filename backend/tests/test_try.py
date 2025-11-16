import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
from httpx import AsyncClient
from app.main import app
from app.routers.auth import get_current_user

# 测试用户凭证
TEST_USER = {
    "email": "user@example.com",
    "password": "string"
}

# ================== Fixtures ==================

@pytest.fixture
def mock_user_email():
    """模拟用户邮箱"""
    return "test@example.com"


@pytest.fixture
def override_get_current_user(mock_user_email):
    """覆盖FastAPI的get_current_user依赖"""
    app.dependency_overrides[get_current_user] = lambda: mock_user_email
    yield mock_user_email
    app.dependency_overrides.clear()


# ================== 测试：创建自定义运动类型 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,expected_status,expected_success", [
    # 正常情况
    ({"sport_type": "自定义跑步", "describe": "户外跑步", "METs": 8.0}, 200, True),
    ({"sport_type": "瑜伽", "describe": "放松瑜伽", "METs": 2.5}, 200, True),
    ({"sport_type": "游泳", "describe": "自由泳", "METs": 9.5}, 200, True),
    # 边界条件 - 最小METs值
    ({"sport_type": "冥想", "describe": "静坐", "METs": 0.1}, 200, True),
    # 边界条件 - 最大METs值
    ({"sport_type": "极限训练", "describe": "高强度", "METs": 20.0}, 200, True),
])
async def test_create_sports(sport_data, expected_status, expected_success):
    """测试创建自定义运动类型 - 正常情况和边界条件"""
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post("/api/sports/create-sport", json=sport_data)
        
    assert response.status_code == expected_status
    result = response.json()
    assert result["success"] == expected_success