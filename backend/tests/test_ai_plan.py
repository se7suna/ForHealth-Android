"""
AI 助手功能测试

测试覆盖：
1. 个性化饮食计划生成
2. 营养知识问答

注意：大模型调用可能需要 60 秒左右，测试超时时间已设置为 120 秒。
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
import pytest_asyncio
from httpx import AsyncClient

from app.config import settings

TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD,
}


@pytest_asyncio.fixture
async def auth_client():
    """创建已认证的客户端"""
    async with AsyncClient(
        base_url="http://127.0.0.1:8000",
        timeout=120.0,  # 设置 120 秒超时，避免因 LLM 响应慢而误判为超时
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
            timeout=120.0,  # 120 秒超时
            http2=False,
        ) as authed_client:
            yield authed_client


@pytest.mark.asyncio
async def test_generate_meal_plan_simple(auth_client: AsyncClient):
    """
    简单测试生成饮食计划功能
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 如果成功生成，验证基本字段存在
    
    注意：此测试会调用真实的 LLM API，响应时间可能较长（60秒左右）
    """
    payload = {
        "plan_duration": "day",
        "plan_days": 1,  # 只生成1天，减少响应时间
        "target_calories": 1800,
        "meals_per_day": 3
    }

    response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=payload
    )
    
    # 验证状态码
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 验证基本字段存在
    assert "success" in data, "响应中应包含 success 字段"
    assert "message" in data, "响应中应包含 message 字段"
    assert "plan_duration" in data, "响应中应包含 plan_duration 字段"
    assert "plan_days" in data, "响应中应包含 plan_days 字段"
    assert "target_calories" in data, "响应中应包含 target_calories 字段"
    assert "daily_plans" in data, "响应中应包含 daily_plans 字段"
    
    # 如果成功生成，验证数据结构
    if data.get("success"):
        assert len(data["daily_plans"]) > 0, "成功时应至少包含一个每日计划"
        daily_plan = data["daily_plans"][0]
        assert "date" in daily_plan, "每日计划应包含 date 字段"
        assert "meals" in daily_plan, "每日计划应包含 meals 字段"
        assert "daily_nutrition" in daily_plan, "每日计划应包含 daily_nutrition 字段"
    else:
        # 如果失败，至少应该有错误信息
        assert "message" in data and data["message"], "失败时应包含错误信息"


@pytest.mark.asyncio
async def test_nutrition_question_simple(auth_client: AsyncClient):
    """
    简单测试营养知识问答功能
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 回答内容经过敏感信息过滤
    
    注意：此测试会调用真实的 LLM API，响应时间可能较长（60秒左右）
    """
    payload = {
        "question": "蛋白质补充的最佳时间是什么时候？"
    }

    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=payload
    )
    
    # 验证状态码
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 验证基本字段存在
    assert "success" in data, "响应中应包含 success 字段"
    assert "question" in data, "响应中应包含 question 字段"
    assert "answer" in data, "响应中应包含 answer 字段"
    
    # 验证问题内容
    assert data["question"] == payload["question"], "返回的问题应与请求一致"
    
    # 如果成功回答，验证回答内容
    if data.get("success"):
        assert len(data["answer"]) > 0, "成功时应包含回答内容"
        # 验证敏感信息已被过滤（不应包含极端建议）
        answer_lower = data["answer"].lower()
        assert "三天瘦十斤" not in answer_lower, "回答中不应包含极端减重建议"
        assert "不吃饭" not in answer_lower or "[已过滤" in data["answer"], "回答中不应包含有害建议"
    else:
        # 如果失败，至少应该有错误信息
        assert "answer" in data and data["answer"], "失败时应包含错误信息"


@pytest.mark.asyncio
async def test_nutrition_question_with_context(auth_client: AsyncClient):
    """
    测试带上下文的营养知识问答
    """
    payload = {
        "question": "我应该每天摄入多少蛋白质？",
        "context": {
            "user_goal": "增肌",
            "activity_level": "high",
            "weight": 70
        }
    }

    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=payload
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    if data.get("success"):
        # 验证回答考虑了上下文（可能提到增肌、活动水平等）
        answer_lower = data["answer"].lower()
        # 注意：这里只是验证接口正常工作，不强制要求回答中必须包含这些关键词
        assert len(data["answer"]) > 0, "成功时应包含回答内容"


@pytest.mark.asyncio
async def test_sports_question_simple(auth_client: AsyncClient):
    """
    简单测试运动知识问答功能
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 回答内容经过敏感信息过滤
    
    注意：此测试会调用真实的 LLM API，响应时间可能较长（60秒左右）
    """
    payload = {
        "question": "如何制定一个有效的减脂运动计划？"
    }

    response = await auth_client.post(
        "/api/ai/sports/ask",
        json=payload
    )
    
    # 验证状态码
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 验证基本字段存在
    assert "success" in data, "响应中应包含 success 字段"
    assert "question" in data, "响应中应包含 question 字段"
    assert "answer" in data, "响应中应包含 answer 字段"
    
    # 验证问题内容
    assert data["question"] == payload["question"], "返回的问题应与请求一致"
    
    # 如果成功回答，验证回答内容
    if data.get("success"):
        assert len(data["answer"]) > 0, "成功时应包含回答内容"
        # 验证敏感信息已被过滤（不应包含极端建议）
        answer_lower = data["answer"].lower()
        assert "一周瘦十斤" not in answer_lower, "回答中不应包含极端减重建议"
        assert "不休息" not in answer_lower or "[已过滤" in data["answer"], "回答中不应包含有害建议"
    else:
        # 如果失败，至少应该有错误信息
        assert "answer" in data and data["answer"], "失败时应包含错误信息"


@pytest.mark.asyncio
async def test_sports_question_with_context(auth_client: AsyncClient):
    """
    测试带上下文的运动知识问答
    """
    payload = {
        "question": "我应该每周运动几次？",
        "context": {
            "user_goal": "减脂",
            "activity_level": "moderately_active",
            "weight": 70,
            "height": 175
        }
    }

    response = await auth_client.post(
        "/api/ai/sports/ask",
        json=payload
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    if data.get("success"):
        # 验证回答考虑了上下文
        assert len(data["answer"]) > 0, "成功时应包含回答内容"
        # 验证相关话题和来源字段
        if data.get("related_topics"):
            assert isinstance(data["related_topics"], list), "相关话题应为列表"
        if data.get("sources"):
            assert isinstance(data["sources"], list), "参考来源应为列表"

