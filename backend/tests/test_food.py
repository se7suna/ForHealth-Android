# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
from httpx import AsyncClient
from datetime import datetime, date
from bson import ObjectId

from app.main import app
from app.routers.auth import get_current_user

import pytest_asyncio

# ========== Fixtures ==========
from app.config import settings
TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD
}

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

@pytest.fixture
def sample_food_data():
    """示例食物数据"""
    return {
        "name": "测试苹果",
        "category": "水果",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 52,
            "protein": 0.3,
            "carbohydrates": 14,
            "fat": 0.2,
            "fiber": 2.4,
            "sugar": 10.4,
            "sodium": 1
        },
        "brand": "测试品牌",
        "barcode": "1234567890123",
        "image_url": "https://example.com/apple.jpg"
    }


# ========== 食物管理测试 ==========

@pytest.mark.asyncio
async def test_create_food_success(auth_client, sample_food_data):
    """测试创建食物 - 成功"""
    expected_success = {
        "status_code": 201,
        "name": "测试苹果",
        "category": "水果"
    }
    
    response = await auth_client.post("/api/food/", json=sample_food_data)
        
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 201:
        data = response.json()
        assert data["name"] == expected_success["name"]
        assert data["category"] == expected_success["category"]
        
        # 测试后清理：删除创建的测试数据
        food_id = data.get("id")
        if food_id:
            await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_search_foods_success(auth_client):
    """测试搜索食物 - 成功"""
    expected_success = {
        "status_code": 200,
        "has_page": True,
        "has_foods": True
    }
      
    response = await auth_client.get("/api/food/search?keyword=苹果&page=1")
        
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "page" in data
        assert "foods" in data
        assert isinstance(data["foods"], list)


@pytest.mark.asyncio
async def test_search_foods_simplified(auth_client):
    """测试搜索食物 - 简化模式"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/food/search?keyword=苹果&simplified=true")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "foods" in data
        assert isinstance(data["foods"], list)


@pytest.mark.asyncio
async def test_search_food_by_name(auth_client):
    """测试按名称搜索食物ID"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/food/search-id?keyword=苹果&limit=20")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "foods" in data


@pytest.mark.asyncio
async def test_get_food(auth_client, sample_food_data):
    """测试获取食物详情"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建一个食物
    create_response = await auth_client.post("/api/food/", json=sample_food_data)
    if create_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = create_response.json().get("id")
    
    # 获取食物详情
    response = await auth_client.get(f"/api/food/{food_id}")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert data["id"] == food_id
        assert data["name"] == sample_food_data["name"]
    
    # 清理
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_update_food(auth_client, sample_food_data):
    """测试更新食物"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建一个食物
    create_response = await auth_client.post("/api/food/", json=sample_food_data)
    if create_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = create_response.json().get("id")
    
    # 更新食物
    update_data = {
        "name": "更新后的苹果",
        "category": "水果"
    }
    response = await auth_client.put(f"/api/food/{food_id}", json=update_data)
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert data["name"] == update_data["name"]
    
    # 清理
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_delete_food(auth_client, sample_food_data):
    """测试删除食物"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建一个食物
    create_response = await auth_client.post("/api/food/", json=sample_food_data)
    if create_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = create_response.json().get("id")
    
    # 删除食物
    response = await auth_client.delete(f"/api/food/{food_id}")
    
    assert response.status_code == expected_success["status_code"]


# ========== 食物记录管理测试 ==========

@pytest.mark.asyncio
async def test_create_food_record(auth_client, sample_food_data):
    """测试创建食物记录"""
    expected_success = {
        "create_food_status": 201,
        "create_record_status": 201
    }

    # 先创建一个食物
    food_response = await auth_client.post("/api/food/", json=sample_food_data)
    assert food_response.status_code == expected_success["create_food_status"]
    
    if food_response.status_code == 201:
        food_id = food_response.json().get("id")
        
        # 创建食物记录
        record_data = {
            "food_id": food_id,
            "serving_amount": 1.5,
            "recorded_at": "2024-01-15T08:30:00",
            "meal_type": "早餐",
            "notes": "测试备注"
        }
        
        record_response = await auth_client.post("/api/food/record", json=record_data)
        assert record_response.status_code == expected_success["create_record_status"]
        
        # 测试后清理：删除创建的测试数据
        if record_response.status_code == 201:
            record_id = record_response.json().get("id")
            if record_id:
                await auth_client.delete(f"/api/food/record/{record_id}")
        
        if food_id:
            await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_get_food_records(auth_client):
    """测试获取食物记录列表"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/food/record/list?limit=100")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "records" in data
        assert "total_nutrition" in data


@pytest.mark.asyncio
async def test_get_daily_nutrition(auth_client):
    """测试获取每日营养摘要"""
    expected_success = {
        "status_code": 200
    }
    
    target_date = date.today()
    
    response = await auth_client.get(f"/api/food/record/daily/{target_date}")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total_calories" in data
        assert "records" in data


@pytest.mark.asyncio
async def test_update_food_record(auth_client, sample_food_data):
    """测试更新食物记录"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建食物和记录
    food_response = await auth_client.post("/api/food/", json=sample_food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食物记录
    record_data = {
        "food_id": food_id,
        "serving_amount": 1.0,
        "recorded_at": "2024-01-15T08:30:00",
        "meal_type": "早餐"
    }
    record_response = await auth_client.post("/api/food/record", json=record_data)
    
    if record_response.status_code == 201:
        record_id = record_response.json().get("id")
        
        # 更新食物记录
        update_data = {
            "serving_amount": 2.0,
            "meal_type": "午餐",
            "notes": "更新后的备注"
        }
        response = await auth_client.put(f"/api/food/record/{record_id}", json=update_data)
        
        assert response.status_code == expected_success["status_code"]
        if response.status_code == 200:
            data = response.json()
            assert data["serving_amount"] == update_data["serving_amount"]
        
        # 清理记录
        if record_id:
            await auth_client.delete(f"/api/food/record/{record_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_delete_food_record(auth_client, sample_food_data):
    """测试删除食物记录"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建食物和记录
    food_response = await auth_client.post("/api/food/", json=sample_food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食物记录
    record_data = {
        "food_id": food_id,
        "serving_amount": 1.0,
        "recorded_at": "2024-01-15T08:30:00",
        "meal_type": "早餐"
    }
    record_response = await auth_client.post("/api/food/record", json=record_data)
    
    if record_response.status_code == 201:
        record_id = record_response.json().get("id")
        
        # 删除食物记录
        response = await auth_client.delete(f"/api/food/record/{record_id}")
        
        assert response.status_code == expected_success["status_code"]
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


# ========== 条形码扫描测试 ==========

@pytest.mark.asyncio
async def test_scan_barcode(auth_client):
    """测试扫描条形码"""
    expected_success = {
        "status_code": 200
    }
    
    barcode = "6901939613702"
    
    response = await auth_client.get(f"/api/food/barcode/{barcode}")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "found" in data
        assert isinstance(data["found"], bool)


@pytest.mark.asyncio
async def test_scan_invalid_barcode(auth_client):
    """测试扫描无效条形码"""
    expected_error = {
        "status_code": 400
    }
    
    barcode = "invalid"
    
    response = await auth_client.get(f"/api/food/barcode/{barcode}")
    
    assert response.status_code == expected_error["status_code"]


@pytest.mark.asyncio
async def test_recognize_barcode_from_image(auth_client):
    """测试从图片识别条形码"""
    expected_success = {
        "status_code": 200,
        "barcode": "6920546800053"
    }
    
    # 读取真实的条形码测试图片
    from pathlib import Path
    
    # 获取测试图片路径
    test_image_path = Path(__file__).parent / "test_picture" / "image1.png"
    
    # 读取图片文件并上传
    with open(test_image_path, "rb") as image_file:
        files = {
            "file": ("image1.png", image_file, "image/png")
        }
        response = await auth_client.post("/api/food/barcode/recognize", files=files)
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        # 验证返回的条形码是否正确
        assert "barcode" in data
        assert data["barcode"] == expected_success["barcode"]


# ========== 未认证测试 ==========

@pytest.mark.asyncio
async def test_create_food_unauthorized():
    """测试创建食物 - 未认证"""
    expected_error = {
        "status_code_range": [401, 403]
    }
    
    food_data = {
        "name": "测试食物",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 10,
            "fat": 3
        }
    }
    
    async with AsyncClient(base_url="http://localhost:8000") as client:
        response = await client.post("/api/food/", json=food_data)
        # 未认证应该返回 401 或 403
        assert response.status_code in expected_error["status_code_range"]
