"""
测试用户 BMR 更新时 TDEE 同步更新的功能
"""

import os
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
import pytest_asyncio
from datetime import date
from httpx import AsyncClient
from app.config import settings
from app.models.user import ActivityLevel, HealthGoalType

# 测试用户凭证
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


@pytest.mark.asyncio
async def test_bmr_update_syncs_tdee(auth_client):
    """
    测试 BMR 更新时 TDEE 同步更新

    测试步骤:
    1. 设置用户的活动水平 (这样会生成初始 TDEE)
    2. 记录初始的 BMR 和 TDEE
    3. 更新用户的身体数据 (体重),这会改变 BMR
    4. 验证 TDEE 也相应更新了
    """

    # Step 1: 设置活动水平
    activity_level_data = {
        "activity_level": ActivityLevel.MODERATELY_ACTIVE.value  # 中等活动
    }
    response = await auth_client.put("/api/user/activity-level", json=activity_level_data)
    assert response.status_code == 200, f"设置活动水平失败: {response.status_code}"

    # 获取初始用户数据
    response = await auth_client.get("/api/user/profile")
    assert response.status_code == 200, f"获取用户资料失败: {response.status_code}"
    initial_profile = response.json()

    initial_bmr = initial_profile.get("bmr")
    initial_tdee = initial_profile.get("tdee")
    initial_weight = initial_profile.get("weight")

    assert initial_bmr is not None, "初始 BMR 不应为空"
    assert initial_tdee is not None, "初始 TDEE 不应为空"
    assert initial_weight is not None, "初始体重不应为空"

    print(f"初始状态 - BMR: {initial_bmr}, TDEE: {initial_tdee}, 体重: {initial_weight}")

    # Step 2: 更新身体数据 (改变体重, 这会改变 BMR)
    new_weight = initial_weight + 5  # 增加 5kg
    body_data = {
        "height": initial_profile.get("height", 170.0),
        "weight": new_weight,
        "birthdate": initial_profile.get("birthdate", "2000-01-01"),
        "gender": initial_profile.get("gender", "male")
    }

    response = await auth_client.put("/api/user/body-data", json=body_data)
    assert response.status_code == 200, f"更新身体数据失败: {response.status_code}"

    # Step 3: 获取更新后的用户数据
    response = await auth_client.get("/api/user/profile")
    assert response.status_code == 200, f"获取更新后用户资料失败: {response.status_code}"
    updated_profile = response.json()

    updated_bmr = updated_profile.get("bmr")
    updated_tdee = updated_profile.get("tdee")
    updated_weight = updated_profile.get("weight")

    assert updated_bmr is not None, "更新后 BMR 不应为空"
    assert updated_tdee is not None, "更新后 TDEE 不应为空"
    assert updated_weight == new_weight, f"体重应更新为 {new_weight}"

    print(f"更新后 - BMR: {updated_bmr}, TDEE: {updated_tdee}, 体重: {updated_weight}")

    # Step 4: 验证 BMR 和 TDEE 都更新了
    assert updated_bmr != initial_bmr, "BMR 应该发生变化"
    assert updated_tdee != initial_tdee, "TDEE 应该发生变化"

    # 验证 TDEE 与 BMR 的关系 (TDEE = BMR × PAL系数)
    # ActivityLevel.MODERATE 的 PAL 系数为 1.55
    expected_tdee = round(updated_bmr * 1.55, 2)
    assert updated_tdee == expected_tdee, f"TDEE 应为 {expected_tdee}, 实际为 {updated_tdee}"

    print("✅ 测试通过: BMR 更新时 TDEE 已同步更新")


@pytest.mark.asyncio
async def test_bmr_update_syncs_daily_calorie_goal(auth_client):
    """
    测试 BMR 更新时,如果用户已设置健康目标,每日卡路里目标也应同步更新

    测试步骤:
    1. 设置用户的活动水平和健康目标
    2. 记录初始的 BMR、TDEE 和每日卡路里目标
    3. 更新用户的身体数据,这会改变 BMR
    4. 验证 TDEE 和每日卡路里目标都相应更新了
    """

    # Step 1: 设置活动水平
    activity_level_data = {
        "activity_level": ActivityLevel.MODERATELY_ACTIVE.value
    }
    response = await auth_client.put("/api/user/activity-level", json=activity_level_data)
    assert response.status_code == 200, f"设置活动水平失败: {response.status_code}"

    # 获取当前用户数据以设置健康目标
    response = await auth_client.get("/api/user/profile")
    assert response.status_code == 200
    profile = response.json()
    current_weight = profile.get("weight", 70.0)

    # Step 2: 设置健康目标
    health_goal_data = {
        "health_goal_type": HealthGoalType.LOSE_WEIGHT.value,  # 减重
        "target_weight": current_weight - 5,
        "goal_period_weeks": 10
    }
    response = await auth_client.put("/api/user/health-goal", json=health_goal_data)
    assert response.status_code == 200, f"设置健康目标失败: {response.status_code}"

    # 获取初始数据
    response = await auth_client.get("/api/user/profile")
    assert response.status_code == 200
    initial_profile = response.json()

    initial_bmr = initial_profile.get("bmr")
    initial_tdee = initial_profile.get("tdee")
    initial_daily_calorie_goal = initial_profile.get("daily_calorie_goal")
    initial_weight = initial_profile.get("weight")

    assert initial_bmr is not None
    assert initial_tdee is not None
    assert initial_daily_calorie_goal is not None

    print(f"初始 - BMR: {initial_bmr}, TDEE: {initial_tdee}, 目标: {initial_daily_calorie_goal}")

    # Step 3: 更新身体数据
    new_weight = initial_weight + 5
    body_data = {
        "height": initial_profile.get("height", 170.0),
        "weight": new_weight,
        "birthdate": initial_profile.get("birthdate", "2000-01-01"),
        "gender": initial_profile.get("gender", "male")
    }

    response = await auth_client.put("/api/user/body-data", json=body_data)
    assert response.status_code == 200

    # Step 4: 验证所有相关值都更新了
    response = await auth_client.get("/api/user/profile")
    assert response.status_code == 200
    updated_profile = response.json()

    updated_bmr = updated_profile.get("bmr")
    updated_tdee = updated_profile.get("tdee")
    updated_daily_calorie_goal = updated_profile.get("daily_calorie_goal")

    # 验证所有值都发生了变化
    assert updated_bmr != initial_bmr, "BMR 应该变化"
    assert updated_tdee != initial_tdee, "TDEE 应该变化"
    assert updated_daily_calorie_goal != initial_daily_calorie_goal, "每日卡路里目标应该变化"

    # 验证数值关系
    expected_tdee = round(updated_bmr * 1.55, 2)
    expected_daily_calorie_goal = round(expected_tdee - 500, 2)  # 减重目标: TDEE - 500

    assert updated_tdee == expected_tdee, f"TDEE 计算错误: 期望 {expected_tdee}, 实际 {updated_tdee}"
    assert updated_daily_calorie_goal == expected_daily_calorie_goal, \
        f"每日卡路里目标计算错误: 期望 {expected_daily_calorie_goal}, 实际 {updated_daily_calorie_goal}"

    print("✅ 测试通过: BMR 更新时所有依赖值都已同步更新")
