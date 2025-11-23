
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
import pytest_asyncio
import json
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
# 加载食物数据集
def load_food_data():
    """加载食物数据集"""
    dataset_path = Path(__file__).parent.parent / "app" / "data_init" / "initial_foods_dataset.json"
    with open(dataset_path, 'r', encoding='utf-8') as f:
        dataset = json.load(f)
    return dataset.get('foods', [])

foods_data = load_food_data()

@pytest.mark.asyncio
@pytest.mark.parametrize("food_data,expected_status", [
    # 测试第一个食物：米饭
    ({"name": foods_data[0]["name"], "category": foods_data[0]["category"], "serving_size": foods_data[0]["serving_size"]}, 200),
    # 测试第二个食物：馒头
    ({"name": foods_data[1]["name"], "category": foods_data[1]["category"], "serving_size": foods_data[1]["serving_size"]}, 200),
])
async def test_initialize_foods_table_success(auth_client, food_data, expected_status):
    """测试初始化食物表是否成功"""
    response = await auth_client.get(f"/api/food/search-id?keyword={food_data.get('name')}&limit=20")
    assert response.status_code == expected_status, f"搜索食物失败: {response.status_code}"
    result = response.json()
    assert "foods" in result, "响应应该包含foods字段"
    assert isinstance(result["foods"], list), "foods应该是列表"
    
    # 查找食物
    exist = False
    for food in result["foods"]:
        if food.get("name") == food_data.get("name"):
            exist = True
            # 判断字段是否存在
            assert "name" in food, "食物应该有name字段"
            assert "source" in food, "食物应该有source字段"
            # 验证字段值
            assert food.get("source") == "local", "食物应该来自本地数据库"
            break
    
    assert exist, f"食物 '{food_data.get('name')}' 不存在"

# ================== 测试：初始化运动类型成功 ==================
@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,expected_status,expected_success", [
    # 正常情况
    ({"sport_type": settings.DefaultSports[0]["sport_type"], "describe": settings.DefaultSports[0]["describe"], "METs": settings.DefaultSports[0]["METs"]}, 200, True),
])
async def test_create_sports(auth_client, sport_data, expected_status, expected_success):
    """测试初始化运动类型是否成功"""
    response = await auth_client.get("/api/sports/get-available-sports-types")
    assert response.status_code == expected_status, f"获取运动类型列表失败: {response.status_code}"
    result = response.json()
    assert isinstance(result, list), "响应应该是列表"
    
    # 查找运动类型
    exist = False
    for sport in result:
        if sport.get("sport_type") == sport_data.get("sport_type"):
            exist = True
            # 判断字段是否存在
            assert "sport_type" in sport, "运动类型应该有sport_type字段"
            assert "METs" in sport, "运动类型应该有METs字段"
            assert "describe" in sport, "运动类型应该有describe字段"
            # 验证字段值
            assert sport.get("describe") == sport_data.get("describe"), "描述不一致"
            assert sport.get("METs") == sport_data.get("METs"), "METs不一致"
            break
    
    assert exist, f"运动类型 '{sport_data.get('sport_type')}' 不存在"
