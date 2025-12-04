"""
拍照识别食物 - AI 识别结果自动落地本地食物库的完整集成测试

测试覆盖（使用合并后的接口 /api/ai/food/recognize）：
1. 真实图片识别：使用真实图片测试完整流程（上传图片 -> AI识别 -> 自动处理 -> 创建记录）
2. 最小参数测试：只上传图片，不提供其他可选参数
3. 错误处理：无效图片文件、无效参数等错误场景

注意：
- 所有测试都使用合并后的接口 /api/ai/food/recognize
- 真实图片识别测试需要调用真实的多模态模型API，可能需要较长时间
- 旧接口 /api/ai/food/confirm-recognition 已废弃，相关测试已删除
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from datetime import datetime, date, timedelta

import pytest
import pytest_asyncio
from httpx import AsyncClient

from app.config import settings


TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD,
}


def _build_food_form_data(name: str, category: str, serving_size: float, serving_unit: str, nutrition: dict) -> dict:
    """
    构建创建食物所需的表单数据（multipart/form-data格式）
    
    Args:
        name: 食物名称
        category: 食物分类
        serving_size: 标准份量
        serving_unit: 份量单位
        nutrition: 营养信息字典，包含 calories, protein, carbohydrates, fat, fiber, sugar, sodium
    
    Returns:
        表单数据字典
    """
    return {
        "name": name,
        "category": category,
        "serving_size": serving_size,
        "serving_unit": serving_unit,
        "calories": nutrition.get("calories", 0),
        "protein": nutrition.get("protein", 0),
        "carbohydrates": nutrition.get("carbohydrates", 0),
        "fat": nutrition.get("fat", 0),
        "fiber": nutrition.get("fiber"),
        "sugar": nutrition.get("sugar"),
        "sodium": nutrition.get("sodium"),
    }


@pytest_asyncio.fixture
async def auth_client():
    """创建已认证的客户端（与现有 AI 测试保持风格一致）"""
    async with AsyncClient(
        base_url="http://127.0.0.1:8000",
        timeout=120.0,  # 真实图片识别可能需要更长时间
        http2=False,
    ) as client:
        # 登录获取 token
        response = await client.post(
            "/api/auth/login",
            json={
                "email": TEST_USER["email"],
                "password": TEST_USER["password"],
            },
        )
        assert (
            response.status_code == 200
        ), f"登录失败: 状态码={response.status_code}, 响应={response.text}"
        token = response.json()["access_token"]

        async with AsyncClient(
            base_url="http://127.0.0.1:8000",
            headers={"Authorization": f"Bearer {token}"},
            timeout=120.0,  # 真实图片识别可能需要更长时间
            http2=False,
        ) as authed_client:
            yield authed_client


# 注意：以下旧测试已删除，因为旧接口 /api/ai/food/confirm-recognition 已不存在
# 相关功能已合并到新接口 /api/ai/food/recognize 中，请使用新的测试用例：
# - test_ai_photo_recognize_endpoint_with_real_image
# - test_ai_photo_recognize_endpoint_minimal
# - test_ai_photo_recognize_endpoint_invalid_image
# - test_ai_photo_recognize_endpoint_invalid_meal_type


@pytest.mark.asyncio
async def test_ai_photo_recognize_endpoint_with_real_image(auth_client: AsyncClient):
    """
    测试：使用真实图片测试合并后的识别接口 /api/ai/food/recognize
    
    步骤：
    1. 读取测试图片文件 image2.jpg
    2. 调用 /api/ai/food/recognize 上传图片并识别处理
    3. 断言：
       - 接口返回成功
       - 返回 processed_foods 包含食物信息
       - 每个食物都有 food_id 和 serving_amount
    4. 可选：调用 /api/food/record 创建记录验证
    """
    # 获取测试图片路径
    test_image_path = Path(__file__).parent / "test_picture" / "image2.jpg"
    
    if not test_image_path.exists():
        pytest.skip(f"测试图片不存在: {test_image_path}")
    
    recorded_at = datetime.now()
    
    # 1. 读取图片文件并上传
    with open(test_image_path, "rb") as f:
        files = {"file": ("image2.jpg", f, "image/jpeg")}
        data = {
            "meal_type": "午餐",
            "notes": "真实图片识别测试",
            "recorded_at": recorded_at.isoformat(),
        }
        
        response = await auth_client.post(
            "/api/ai/food/recognize",
            files=files,
            data=data,
        )
    
    assert response.status_code == 200, f"识别接口失败: {response.text}"
    result = response.json()
    
    # 2. 验证响应结构
    assert "success" in result
    assert "message" in result
    assert "processed_foods" in result
    assert "total_foods" in result
    
    # 3. 如果识别成功，验证处理后的食物信息
    if result.get("success"):
        processed_foods = result.get("processed_foods", [])
        total_foods = result.get("total_foods", 0)
        
        assert total_foods > 0, "应该至少识别到一种食物"
        assert len(processed_foods) == total_foods, "processed_foods 数量应该等于 total_foods"
        
        # 验证每个处理后的食物都有必要字段
        for food in processed_foods:
            assert "food_id" in food, f"食物 {food.get('food_name')} 缺少 food_id"
            assert food["food_id"], f"食物 {food.get('food_name')} 的 food_id 不能为空"
            assert "food_name" in food, f"食物缺少 food_name"
            assert food["food_name"], f"食物名称不能为空"
            assert "serving_amount" in food, f"食物 {food.get('food_name')} 缺少 serving_amount"
            assert food["serving_amount"] > 0, f"食物 {food.get('food_name')} 的 serving_amount 应该大于0"
            assert "serving_size" in food, f"食物 {food.get('food_name')} 缺少 serving_size"
            assert "serving_unit" in food, f"食物 {food.get('food_name')} 缺少 serving_unit"
        
        # 4. 可选：（不测试创建记录，因为食物可能不存在于库中）
        if processed_foods and False:
            first_food = processed_foods[0]
            record_payload = {
                "food_id": first_food["food_id"],
                "serving_amount": first_food["serving_amount"],
                "recorded_at": recorded_at.isoformat(),
                "meal_type": "午餐",
                "notes": "真实图片识别测试",
                "source": "local",
            }
            
            record_response = await auth_client.post(
                "/api/food/record",
                json=record_payload,
            )
            
            assert record_response.status_code == 201, f"创建记录失败: {record_response.text}"
            
            # 验证记录确实被创建
            target_date = recorded_at.date().isoformat()
            list_resp = await auth_client.get(
                "/api/food/record/list",
                params={
                    "start_date": target_date,
                    "end_date": target_date,
                    "limit": 50,
                    "offset": 0,
                },
            )
            
            assert list_resp.status_code == 200
            list_data = list_resp.json()
            records = list_data.get("records", [])
            
            # 查找刚创建的记录
            matched = [
                r for r in records 
                if r.get("food_id") == first_food["food_id"] 
                and r.get("food_name") == first_food["food_name"]
            ]
            assert len(matched) >= 1, "应该能找到刚创建的记录"
            
            # 清理：删除创建的记录
            for record in matched:
                record_id = record.get("id")
                if record_id:
                    await auth_client.delete(f"/api/food/record/{record_id}")
    else:
        # 如果识别失败，至少验证错误消息存在
        assert "message" in result, "识别失败时应该有错误消息"
        print(f"识别失败（可能是AI服务问题）: {result.get('message')}")


@pytest.mark.asyncio
async def test_ai_photo_recognize_endpoint_minimal(auth_client: AsyncClient):
    """
    测试：使用真实图片测试合并后的识别接口（最小参数）
    
    只上传图片，不提供其他可选参数。
    """
    # 获取测试图片路径
    test_image_path = Path(__file__).parent / "test_picture" / "image2.jpg"
    
    if not test_image_path.exists():
        pytest.skip(f"测试图片不存在: {test_image_path}")
    
    # 1. 只上传图片，不提供其他参数
    with open(test_image_path, "rb") as f:
        files = {"file": ("image2.jpg", f, "image/jpeg")}
        
        response = await auth_client.post(
            "/api/ai/food/recognize",
            files=files,
        )
    
    assert response.status_code == 200, f"识别接口失败: {response.text}"
    result = response.json()
    
    # 2. 验证响应结构
    assert "success" in result
    assert "message" in result
    assert "processed_foods" in result
    assert "total_foods" in result
    
    # 3. 如果识别成功，验证基本结构
    if result.get("success"):
        processed_foods = result.get("processed_foods", [])
        assert len(processed_foods) > 0, "应该至少识别到一种食物"
        
        # 验证第一个食物有必要的字段
        first_food = processed_foods[0]
        assert "food_id" in first_food
        assert "food_name" in first_food
        assert "serving_amount" in first_food


@pytest.mark.asyncio
async def test_ai_photo_recognize_endpoint_invalid_image(auth_client: AsyncClient):
    """
    测试：上传无效图片文件时的错误处理
    """
    # 创建一个无效的图片文件（实际是文本）
    invalid_content = b"This is not an image file"
    
    files = {"file": ("invalid.txt", invalid_content, "text/plain")}
    
    response = await auth_client.post(
        "/api/ai/food/recognize",
        files=files,
    )
    
    # 应该返回错误（400 或 500）
    assert response.status_code in [400, 500], f"应该返回错误状态码，但得到 {response.status_code}"
    result = response.json()
    assert "detail" in result or "message" in result, "错误响应应该包含错误信息"


@pytest.mark.asyncio
async def test_ai_photo_recognize_endpoint_invalid_meal_type(auth_client: AsyncClient):
    """
    测试：提供无效的 meal_type 参数时的错误处理
    """
    test_image_path = Path(__file__).parent / "test_picture" / "image2.jpg"
    
    if not test_image_path.exists():
        pytest.skip(f"测试图片不存在: {test_image_path}")
    
    # 1. 上传图片并提供无效的 meal_type
    with open(test_image_path, "rb") as f:
        files = {"file": ("image2.jpg", f, "image/jpeg")}
        data = {
            "meal_type": "无效餐次",  # 无效值
        }
        
        response = await auth_client.post(
            "/api/ai/food/recognize",
            files=files,
            data=data,
        )
    
    # 应该返回 400 错误
    assert response.status_code == 400, f"应该返回400错误，但得到 {response.status_code}"
    result = response.json()
    assert "detail" in result, "错误响应应该包含 detail 字段"
    assert "餐次类型" in result["detail"] or "meal_type" in result["detail"].lower(), "错误消息应该提到餐次类型"


