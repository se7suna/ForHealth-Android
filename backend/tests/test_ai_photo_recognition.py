"""
拍照识别食物 - AI 识别结果自动落地本地食物库的完整集成测试

测试覆盖：
1. 纯 AI 结果自动落地：AI识别到新食物（无food_id），自动创建本地食物并生成记录
2. 已存在食物识别：AI识别到已存在食物（food_id=None但名称匹配），使用已有food_id，不重复创建
3. 已匹配食物确认：AI识别结果已包含food_id（识别阶段已匹配），直接使用该food_id创建记录
4. 混合场景：同时识别到已存在食物和纯AI食物，都能正确处理

本测试不依赖真实多模态模型调用，直接构造 AI 识别结果请求体进行测试。
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
        timeout=30.0,
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
            timeout=30.0,
            http2=False,
        ) as authed_client:
            yield authed_client


@pytest.mark.asyncio
async def test_ai_photo_pure_ai_flow_creates_record(auth_client: AsyncClient):
    """
    测试：纯 AI 识别结果（无 food_id）也会自动创建本地食物并生成一条饮食记录。

    步骤：
    1. 构造一个仅包含 AI 结果的确认请求（source='ai'，food_id=None）；
    2. 调用 /api/ai/food/confirm-recognition；
    3. 断言：
       - 接口 success == True，total_records == 1；
       - 返回的 created_records 非空；
    4. 再调用 /api/food/record/list，确认记录确实写入（且带有 food_id）。
    """

    recorded_at = datetime.now()
    food_name = "测试AI香蕉"

    confirm_payload = {
        "recognized_foods": [
            {
                "food_name": food_name,
                "serving_size": 120,
                "serving_unit": "克",
                "nutrition_per_serving": {
                    "calories": 105,
                    "protein": 1.3,
                    "carbohydrates": 27,
                    "fat": 0.3,
                    "fiber": 3.1,
                    "sugar": 14.4,
                    "sodium": 1,
                },
                # 关键：不提供 food_id，source=ai
                "food_id": None,
                "source": "ai",
            }
        ],
        "recorded_at": recorded_at.isoformat(),
        "meal_type": "午餐",
        "notes": "AI 纯识别结果自动落地测试",
    }

    # 1. 调用确认接口
    response = await auth_client.post(
        "/api/ai/food/confirm-recognition",
        json=confirm_payload,
    )

    assert response.status_code == 200
    data = response.json()

    assert data.get("success") is True
    assert data.get("total_records") == 1
    created_records = data.get("created_records") or []
    assert len(created_records) == 1

    # 2. 从饮食记录列表中确认这条记录确实被写入，并且带有 food_id
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

    records = list_data.get("records") or []
    # 按名称筛选我们刚才创建的记录
    matched = [r for r in records if r.get("food_name") == food_name]
    assert (
        len(matched) >= 1
    ), f"未在食物记录列表中找到名称为 {food_name} 的记录: {records}"

    # 至少有一条记录带有 food_id（说明已经落地到本地食物库并建立关联）
    assert any(r.get("food_id") for r in matched), "纯 AI 食物记录未关联 food_id"


@pytest.mark.asyncio
async def test_ai_photo_existing_food_creates_record(auth_client: AsyncClient):
    """
    测试：AI识别到已存在数据库中的食物，应该能成功识别并写入记录（使用已有food_id）。

    步骤：
    1. 先创建一个测试食物（模拟数据库中已存在）；
    2. 构造一个AI识别结果，food_name与已存在食物相同，但food_id为None（模拟AI识别结果）；
    3. 调用 /api/ai/food/confirm-recognition；
    4. 断言：
       - 接口 success == True，total_records == 1；
       - 返回的 created_records 非空；
    5. 验证不会重复创建食物（查询食物列表，确认只有一条该名称的食物）；
    6. 验证记录使用的是已有food_id。
    """
    recorded_at = datetime.now()
    food_name = "测试已存在苹果"

    # 1. 先创建一个测试食物（模拟数据库中已存在）
    existing_food_form = _build_food_form_data(
        name=food_name,
        category="水果",
        serving_size=100,
        serving_unit="克",
        nutrition={
            "calories": 52,
            "protein": 0.3,
            "carbohydrates": 14,
            "fat": 0.2,
            "fiber": 2.4,
            "sugar": 10.4,
            "sodium": 1,
        },
    )

    create_response = await auth_client.post("/api/food/", data=existing_food_form)
    assert create_response.status_code == 201, f"创建测试食物失败: {create_response.text}"
    existing_food_id = create_response.json().get("id")
    assert existing_food_id, "创建的食物没有返回ID"

    try:
        # 2. 构造AI识别结果（food_name相同，但food_id为None，模拟AI识别结果）
        confirm_payload = {
            "recognized_foods": [
                {
                    "food_name": food_name,  # 与已存在食物名称相同
                    "serving_size": 150,  # AI识别的份量
                    "serving_unit": "克",
                    "nutrition_per_serving": {
                        "calories": 78,  # AI识别的营养信息（可能与数据库不同）
                        "protein": 0.45,
                        "carbohydrates": 21,
                        "fat": 0.3,
                        "fiber": 3.6,
                        "sugar": 15.6,
                        "sodium": 1.5,
                    },
                    # 关键：不提供food_id，模拟纯AI识别结果
                    "food_id": None,
                    "source": "ai",
                }
            ],
            "recorded_at": recorded_at.isoformat(),
            "meal_type": "午餐",
            "notes": "AI识别已存在食物测试",
        }

        # 3. 调用确认接口
        response = await auth_client.post(
            "/api/ai/food/confirm-recognition",
            json=confirm_payload,
        )

        assert response.status_code == 200, f"确认接口失败: {response.text}"
        data = response.json()

        assert data.get("success") is True, f"确认失败: {data.get('message')}"
        assert data.get("total_records") == 1
        created_records = data.get("created_records") or []
        assert len(created_records) == 1

        # 4. 验证不会重复创建食物：查询食物列表，确认只有一条该名称的食物
        search_response = await auth_client.get(
            "/api/food/search-id",
            params={"keyword": food_name, "limit": 10},
        )
        assert search_response.status_code == 200
        search_data = search_response.json()
        foods_with_same_name = [
            f for f in search_data.get("foods", []) if f.get("name") == food_name
        ]
        assert (
            len(foods_with_same_name) == 1
        ), f"应该只有一条名为 {food_name} 的食物，但找到 {len(foods_with_same_name)} 条: {foods_with_same_name}"

        # 5. 验证记录使用的是已有food_id
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

        records = list_data.get("records") or []
        matched = [r for r in records if r.get("food_name") == food_name]
        assert len(matched) >= 1, f"未在食物记录列表中找到名称为 {food_name} 的记录"

        # 验证记录使用的food_id就是之前创建的食物ID
        record_food_id = matched[0].get("food_id")
        assert (
            record_food_id == existing_food_id
        ), f"记录应该使用已有food_id {existing_food_id}，但实际使用了 {record_food_id}"

    finally:
        # 清理：删除创建的记录和食物
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
        if list_resp.status_code == 200:
            records = list_resp.json().get("records") or []
            for record in records:
                if record.get("food_name") == food_name:
                    record_id = record.get("id")
                    if record_id:
                        await auth_client.delete(f"/api/food/record/{record_id}")

        # 删除测试食物
        if existing_food_id:
            await auth_client.delete(f"/api/food/{existing_food_id}")


@pytest.mark.asyncio
async def test_ai_photo_existing_food_with_food_id_creates_record(auth_client: AsyncClient):
    """
    测试：AI识别结果中已包含food_id（识别阶段已匹配到数据库），应该直接使用该food_id创建记录。

    步骤：
    1. 先创建一个测试食物；
    2. 构造一个AI识别结果，food_id指向已存在的食物（模拟识别阶段已匹配）；
    3. 调用 /api/ai/food/confirm-recognition；
    4. 断言：
       - 接口 success == True，total_records == 1；
       - 返回的 created_records 非空；
    5. 验证记录使用的是提供的food_id，且不会创建新食物。
    """
    recorded_at = datetime.now()
    food_name = "测试匹配苹果"

    # 1. 先创建一个测试食物
    existing_food_form = _build_food_form_data(
        name=food_name,
        category="水果",
        serving_size=100,
        serving_unit="克",
        nutrition={
            "calories": 52,
            "protein": 0.3,
            "carbohydrates": 14,
            "fat": 0.2,
            "fiber": 2.4,
            "sugar": 10.4,
            "sodium": 1,
        },
    )

    create_response = await auth_client.post("/api/food/", data=existing_food_form)
    assert create_response.status_code == 201, f"创建测试食物失败: {create_response.text}"
    existing_food_id = create_response.json().get("id")
    assert existing_food_id, "创建的食物没有返回ID"

    try:
        # 2. 构造AI识别结果（已包含food_id，模拟识别阶段已匹配到数据库）
        confirm_payload = {
            "recognized_foods": [
                {
                    "food_name": food_name,
                    "serving_size": 150,
                    "serving_unit": "克",
                    "nutrition_per_serving": {
                        "calories": 78,
                        "protein": 0.45,
                        "carbohydrates": 21,
                        "fat": 0.3,
                        "fiber": 3.6,
                        "sugar": 15.6,
                        "sodium": 1.5,
                    },
                    # 关键：已提供food_id（模拟识别阶段已匹配）
                    "food_id": existing_food_id,
                    "source": "database",
                }
            ],
            "recorded_at": recorded_at.isoformat(),
            "meal_type": "晚餐",
            "notes": "AI识别已匹配食物测试",
        }

        # 3. 调用确认接口
        response = await auth_client.post(
            "/api/ai/food/confirm-recognition",
            json=confirm_payload,
        )

        assert response.status_code == 200, f"确认接口失败: {response.text}"
        data = response.json()

        assert data.get("success") is True, f"确认失败: {data.get('message')}"
        assert data.get("total_records") == 1
        created_records = data.get("created_records") or []
        assert len(created_records) == 1

        # 4. 验证记录使用的是提供的food_id
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

        records = list_data.get("records") or []
        matched = [r for r in records if r.get("food_name") == food_name]
        assert len(matched) >= 1, f"未在食物记录列表中找到名称为 {food_name} 的记录"

        # 验证记录使用的food_id就是提供的food_id
        record_food_id = matched[0].get("food_id")
        assert (
            record_food_id == existing_food_id
        ), f"记录应该使用food_id {existing_food_id}，但实际使用了 {record_food_id}"

        # 5. 验证不会创建新食物（查询食物列表，确认只有一条）
        search_response = await auth_client.get(
            "/api/food/search-id",
            params={"keyword": food_name, "limit": 10},
        )
        assert search_response.status_code == 200
        search_data = search_response.json()
        foods_with_same_name = [
            f for f in search_data.get("foods", []) if f.get("name") == food_name
        ]
        assert (
            len(foods_with_same_name) == 1
        ), f"应该只有一条名为 {food_name} 的食物，但找到 {len(foods_with_same_name)} 条"

    finally:
        # 清理：删除创建的记录和食物
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
        if list_resp.status_code == 200:
            records = list_resp.json().get("records") or []
            for record in records:
                if record.get("food_name") == food_name:
                    record_id = record.get("id")
                    if record_id:
                        await auth_client.delete(f"/api/food/record/{record_id}")

        # 删除测试食物
        if existing_food_id:
            await auth_client.delete(f"/api/food/{existing_food_id}")


@pytest.mark.asyncio
async def test_ai_photo_mixed_foods_creates_records(auth_client: AsyncClient):
    """
    测试：混合场景 - 同时识别到已存在食物和纯AI食物，都能正确处理。

    步骤：
    1. 先创建一个测试食物（已存在）；
    2. 构造包含两种类型的识别结果：
       - 一个已存在食物（food_id=None，但名称匹配）
       - 一个纯AI食物（food_id=None，名称不匹配）
    3. 调用 /api/ai/food/confirm-recognition；
    4. 断言：
       - 接口 success == True，total_records == 2；
       - 已存在食物使用已有food_id，纯AI食物创建新食物并关联food_id。
    """
    recorded_at = datetime.now()
    existing_food_name = "测试混合已存在苹果"
    new_food_name = "测试混合新食物香蕉"

    # 1. 先创建一个测试食物（已存在）
    existing_food_form = _build_food_form_data(
        name=existing_food_name,
        category="水果",
        serving_size=100,
        serving_unit="克",
        nutrition={
            "calories": 52,
            "protein": 0.3,
            "carbohydrates": 14,
            "fat": 0.2,
            "fiber": 2.4,
            "sugar": 10.4,
            "sodium": 1,
        },
    )

    create_response = await auth_client.post("/api/food/", data=existing_food_form)
    assert create_response.status_code == 201, f"创建测试食物失败: {create_response.text}"
    existing_food_id = create_response.json().get("id")
    assert existing_food_id, "创建的食物没有返回ID"

    try:
        # 2. 构造混合识别结果
        confirm_payload = {
            "recognized_foods": [
                {
                    # 已存在食物（food_id=None，但名称匹配）
                    "food_name": existing_food_name,
                    "serving_size": 150,
                    "serving_unit": "克",
                    "nutrition_per_serving": {
                        "calories": 78,
                        "protein": 0.45,
                        "carbohydrates": 21,
                        "fat": 0.3,
                        "fiber": 3.6,
                        "sugar": 15.6,
                        "sodium": 1.5,
                    },
                    "food_id": None,
                    "source": "ai",
                },
                {
                    # 纯AI食物（food_id=None，名称不匹配）
                    "food_name": new_food_name,
                    "serving_size": 120,
                    "serving_unit": "克",
                    "nutrition_per_serving": {
                        "calories": 105,
                        "protein": 1.3,
                        "carbohydrates": 27,
                        "fat": 0.3,
                        "fiber": 3.1,
                        "sugar": 14.4,
                        "sodium": 1,
                    },
                    "food_id": None,
                    "source": "ai",
                },
            ],
            "recorded_at": recorded_at.isoformat(),
            "meal_type": "早餐",
            "notes": "混合场景测试",
        }

        # 3. 调用确认接口
        response = await auth_client.post(
            "/api/ai/food/confirm-recognition",
            json=confirm_payload,
        )

        assert response.status_code == 200, f"确认接口失败: {response.text}"
        data = response.json()

        assert data.get("success") is True, f"确认失败: {data.get('message')}"
        assert data.get("total_records") == 2, f"应该创建2条记录，但实际创建了 {data.get('total_records')} 条"

        # 4. 验证记录
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

        records = list_data.get("records") or []
        existing_records = [r for r in records if r.get("food_name") == existing_food_name]
        new_records = [r for r in records if r.get("food_name") == new_food_name]

        assert len(existing_records) >= 1, f"未找到已存在食物的记录"
        assert len(new_records) >= 1, f"未找到新食物的记录"

        # 验证已存在食物使用已有food_id
        existing_record_food_id = existing_records[0].get("food_id")
        assert (
            existing_record_food_id == existing_food_id
        ), f"已存在食物记录应该使用food_id {existing_food_id}，但实际使用了 {existing_record_food_id}"

        # 验证新食物创建了新food_id
        new_record_food_id = new_records[0].get("food_id")
        assert new_record_food_id, "新食物记录应该有关联的food_id"
        assert (
            new_record_food_id != existing_food_id
        ), "新食物应该有不同的food_id"

        # 验证不会重复创建已存在食物
        search_response = await auth_client.get(
            "/api/food/search-id",
            params={"keyword": existing_food_name, "limit": 10},
        )
        assert search_response.status_code == 200
        search_data = search_response.json()
        foods_with_same_name = [
            f for f in search_data.get("foods", []) if f.get("name") == existing_food_name
        ]
        assert (
            len(foods_with_same_name) == 1
        ), f"应该只有一条名为 {existing_food_name} 的食物，但找到 {len(foods_with_same_name)} 条"

    finally:
        # 清理：删除创建的记录和食物
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
        if list_resp.status_code == 200:
            records = list_resp.json().get("records") or []
            for record in records:
                record_name = record.get("food_name")
                if record_name in (existing_food_name, new_food_name):
                    record_id = record.get("id")
                    if record_id:
                        await auth_client.delete(f"/api/food/record/{record_id}")

        # 删除测试食物
        if existing_food_id:
            await auth_client.delete(f"/api/food/{existing_food_id}")

        # 删除新创建的食物
        search_response = await auth_client.get(
            "/api/food/search-id",
            params={"keyword": new_food_name, "limit": 10},
        )
        if search_response.status_code == 200:
            search_data = search_response.json()
            for food in search_data.get("foods", []):
                if food.get("name") == new_food_name:
                    new_food_id = food.get("food_id")
                    if new_food_id:
                        await auth_client.delete(f"/api/food/{new_food_id}")


