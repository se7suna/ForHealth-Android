"""
食谱管理和食谱记录测试模块

测试覆盖：
1. 食谱管理：创建、搜索、获取详情、删除
2. 食谱记录管理：创建、查询
"""

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from httpx import AsyncClient

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
def sample_recipe_data():
    """示例食谱数据 - 用于创建食谱"""
    return {
        "name": "测试食谱早餐",
        "description": "营养均衡的早餐组合",
        "category": "早餐",
        "foods": [],  # 需要在测试中添加真实的食物
        "tags": ["早餐", "健康", "简单"],
        "image_url": "https://example.com/breakfast.jpg",
        "prep_time": 15
    }


# ========== 食谱管理测试 ==========

@pytest.mark.asyncio
async def test_create_recipe_success(auth_client, sample_recipe_data):
    """测试创建食谱 - 成功"""
    expected_success = {
        "status_code": 201,
        "name": "测试食谱早餐",
        "category": "早餐"
    }
    
    # 创建测试食物
    food_data = {
        "name": "测试食物_食谱用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱（包含完整的食物信息）
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_食谱用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    response = await auth_client.post("/api/recipe/", json=recipe_data)
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 201:
        data = response.json()
        assert data["name"] == expected_success["name"]
        assert data["category"] == expected_success["category"]
        
        # 清理：删除创建的食谱和食物
        recipe_id = data.get("id")
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_search_recipes_success(auth_client):
    """测试搜索食谱 - 成功"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/recipe/search?keyword=早餐&limit=20")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "recipes" in data
        assert isinstance(data["recipes"], list)


@pytest.mark.asyncio
async def test_search_recipe_by_name(auth_client):
    """测试按名称搜索食谱"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/recipe/search-id?keyword=早餐&limit=20")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "recipes" in data


@pytest.mark.asyncio
async def test_get_recipe_categories_success(auth_client):
    """测试获取食谱分类 - 成功"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/recipe/categories")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert isinstance(data, list)


@pytest.mark.asyncio
async def test_get_recipe(auth_client, sample_recipe_data):
    """测试获取食谱详情"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建测试食物和食谱
    food_data = {
        "name": "测试食物_获取食谱用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_获取食谱用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 获取食谱详情
        response = await auth_client.get(f"/api/recipe/{recipe_id}")
        
        assert response.status_code == expected_success["status_code"]
        if response.status_code == 200:
            data = response.json()
            assert data["id"] == recipe_id
            assert data["name"] == sample_recipe_data["name"]
        
        # 清理食谱
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_update_recipe(auth_client, sample_recipe_data):
    """测试更新食谱"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建测试食物和食谱
    food_data = {
        "name": "测试食物_更新食谱用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_更新食谱用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 更新食谱
        update_data = {
            "name": "更新后的食谱名称",
            "description": "更新后的描述"
        }
        response = await auth_client.put(f"/api/recipe/{recipe_id}", json=update_data)
        
        assert response.status_code == expected_success["status_code"]
        if response.status_code == 200:
            data = response.json()
            assert data["name"] == update_data["name"]
        
        # 清理食谱
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_delete_recipe(auth_client, sample_recipe_data):
    """测试删除食谱"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建测试食物和食谱
    food_data = {
        "name": "测试食物_删除食谱用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_删除食谱用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 删除食谱
        response = await auth_client.delete(f"/api/recipe/{recipe_id}")
        
        assert response.status_code == expected_success["status_code"]
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


# ========== 食谱记录管理测试 ==========

@pytest.mark.asyncio
async def test_create_recipe_record(auth_client, sample_recipe_data):
    """测试创建食谱记录"""
    expected_success = {
        "status_code": 201
    }
    
    # 先创建测试食物和食谱
    food_data = {
        "name": "测试食物_食谱记录用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_食谱记录用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 创建食谱记录
        record_data = {
            "recipe_id": recipe_id,
            "scale": 1.0,
            "recorded_at": "2024-01-15T08:30:00",
            "meal_type": "早餐",
            "notes": "测试食谱记录"
        }
        
        response = await auth_client.post("/api/recipe/record", json=record_data)
        
        assert response.status_code == expected_success["status_code"]
        if response.status_code == 201:
            data = response.json()
            batch_id = data.get("batch_id")
            
            # 清理记录
            if batch_id:
                await auth_client.delete(f"/api/recipe/record/{batch_id}")
        
        # 清理食谱
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_get_recipe_records_success(auth_client):
    """测试获取食谱记录列表 - 成功"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/recipe/record?limit=100")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "batches" in data
        assert isinstance(data["batches"], list)


@pytest.mark.asyncio
async def test_update_recipe_record(auth_client, sample_recipe_data):
    """测试更新食谱记录"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建测试食物、食谱和记录
    food_data = {
        "name": "测试食物_更新记录用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_更新记录用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 创建食谱记录
        record_data = {
            "recipe_id": recipe_id,
            "scale": 1.0,
            "recorded_at": "2024-01-15T08:30:00",
            "meal_type": "早餐"
        }
        
        record_response = await auth_client.post("/api/recipe/record", json=record_data)
        if record_response.status_code == 201:
            batch_id = record_response.json().get("batch_id")
            
            # 更新食谱记录
            update_data = {
                "meal_type": "午餐",
                "notes": "更新后的备注"
            }
            response = await auth_client.put(f"/api/recipe/record/{batch_id}", json=update_data)
            
            assert response.status_code == expected_success["status_code"]
            
            # 清理记录
            if batch_id:
                await auth_client.delete(f"/api/recipe/record/{batch_id}")
        
        # 清理食谱
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_delete_recipe_record(auth_client, sample_recipe_data):
    """测试删除食谱记录"""
    expected_success = {
        "status_code": 200
    }
    
    # 先创建测试食物、食谱和记录
    food_data = {
        "name": "测试食物_删除记录用",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }
    
    food_response = await auth_client.post("/api/food/", json=food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 创建食谱
    recipe_data = sample_recipe_data.copy()
    recipe_data["foods"] = [{
        "food_id": food_id,
        "food_name": "测试食物_删除记录用",
        "serving_amount": 1.0,
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition": {
            "calories": 100,
            "protein": 5,
            "carbohydrates": 20,
            "fat": 2
        }
    }]
    
    recipe_response = await auth_client.post("/api/recipe/", json=recipe_data)
    if recipe_response.status_code == 201:
        recipe_id = recipe_response.json().get("id")
        
        # 创建食谱记录
        record_data = {
            "recipe_id": recipe_id,
            "scale": 1.0,
            "recorded_at": "2024-01-15T08:30:00",
            "meal_type": "早餐"
        }
        
        record_response = await auth_client.post("/api/recipe/record", json=record_data)
        if record_response.status_code == 201:
            batch_id = record_response.json().get("batch_id")
            
            # 删除食谱记录
            response = await auth_client.delete(f"/api/recipe/record/{batch_id}")
            
            assert response.status_code == expected_success["status_code"]
        
        # 清理食谱
        if recipe_id:
            await auth_client.delete(f"/api/recipe/{recipe_id}")
    
    # 清理食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")
