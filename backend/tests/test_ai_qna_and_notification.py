"""
AI 助手功能测试 - 健康知识问答与智能推荐

测试覆盖：
1. 统一健康知识问答接口（营养、运动、综合健康问题）
2. 饮食分析与建议接口
3. 智能菜式推荐接口

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
async def test_health_question_nutrition(auth_client: AsyncClient):
    """
    测试健康知识问答 - 营养类问题
    
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
        "/api/ai/ask",
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
async def test_health_question_sports(auth_client: AsyncClient):
    """
    测试健康知识问答 - 运动类问题
    
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
        "/api/ai/ask",
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
async def test_health_question_with_context(auth_client: AsyncClient):
    """
    测试带上下文的健康知识问答
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
        "/api/ai/ask",
        json=payload
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    if data.get("success"):
        # 验证回答考虑了上下文（可能提到增肌、活动水平等）
        assert len(data["answer"]) > 0, "成功时应包含回答内容"
        # 验证相关话题和来源字段
        if data.get("related_topics"):
            assert isinstance(data["related_topics"], list), "相关话题应为列表"
        if data.get("sources"):
            assert isinstance(data["sources"], list), "参考来源应为列表"


@pytest.mark.asyncio
async def test_health_question_comprehensive(auth_client: AsyncClient):
    """
    测试综合健康问题（涉及营养和运动）
    """
    payload = {
        "question": "减脂期间应该如何搭配饮食和运动？",
        "context": {
            "user_goal": "减脂",
            "activity_level": "moderately_active",
            "weight": 75,
            "height": 170
        }
    }

    response = await auth_client.post(
        "/api/ai/ask",
        json=payload
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    if data.get("success"):
        # 验证回答考虑了上下文
        assert len(data["answer"]) > 0, "成功时应包含回答内容"
        # 综合问题可能涉及多个话题
        answer = data["answer"]
        # 回答应该涵盖相关内容（不强制要求，只是验证接口工作正常）
        assert len(answer) > 50, "综合问题的回答应该有一定长度"


# ========== 饮食分析与建议测试 ==========

@pytest.mark.asyncio
async def test_diet_analyze_default(auth_client: AsyncClient):
    """
    测试饮食分析接口 - 默认参数（分析最近7天）
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. message 字段包含亲和的建议或鼓励
    """
    response = await auth_client.post(
        "/api/ai/diet/analyze",
        json={"days": 7}
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 验证基本字段
    assert "success" in data, "响应中应包含 success 字段"
    assert "message" in data, "响应中应包含 message 字段"
    assert data["success"] is True, "接口应返回成功"
    
    # 验证 message 不为空
    assert len(data["message"]) > 0, "message 应包含建议或鼓励内容"
    
    # 验证 analysis 字段（如果有）
    if data.get("analysis"):
        analysis = data["analysis"]
        assert "days_analyzed" in analysis, "analysis 应包含 days_analyzed"
        
        # 如果有记录，验证更多字段
        if analysis.get("records_count", 0) > 0:
            assert "avg_calories_intake" in analysis, "有记录时应包含 avg_calories_intake"
            assert "calorie_balance" in analysis, "有记录时应包含 calorie_balance"
            assert "macro_ratio" in analysis, "有记录时应包含 macro_ratio"
            
            macro_ratio = analysis["macro_ratio"]
            assert "protein_percent" in macro_ratio, "macro_ratio 应包含 protein_percent"
            assert "carbs_percent" in macro_ratio, "macro_ratio 应包含 carbs_percent"
            assert "fat_percent" in macro_ratio, "macro_ratio 应包含 fat_percent"


@pytest.mark.asyncio
async def test_diet_analyze_custom_days(auth_client: AsyncClient):
    """
    测试饮食分析接口 - 自定义天数
    """
    response = await auth_client.post(
        "/api/ai/diet/analyze",
        json={"days": 3}
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    assert data["success"] is True
    assert len(data["message"]) > 0
    
    if data.get("analysis"):
        # 验证天数设置正确
        assert data["analysis"]["days_analyzed"] == 3


@pytest.mark.asyncio
async def test_diet_analyze_no_records(auth_client: AsyncClient):
    """
    测试饮食分析接口 - 无记录情况
    
    即使没有饮食记录，接口也应该正常返回并给出友好提示
    """
    # 使用一个很短的时间范围，可能没有记录
    response = await auth_client.post(
        "/api/ai/diet/analyze",
        json={"days": 1}
    )
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 无论有无记录，都应该成功返回
    assert data["success"] is True
    # message 应该有内容（提示开始记录或给出建议）
    assert len(data["message"]) > 0


# ========== 智能菜式推荐测试 ==========

@pytest.mark.asyncio
async def test_meal_recommend(auth_client: AsyncClient):
    """
    测试智能菜式推荐接口
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 推荐内容包含时间相关的问候和菜式推荐
    """
    response = await auth_client.get("/api/ai/meal/recommend")
    
    assert response.status_code == 200, f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    
    # 验证基本字段
    assert "success" in data, "响应中应包含 success 字段"
    assert "message" in data, "响应中应包含 message 字段"
    assert "meal_type" in data, "响应中应包含 meal_type 字段"
    assert "recommended_dish" in data, "响应中应包含 recommended_dish 字段"
    assert "reason" in data, "响应中应包含 reason 字段"
    
    assert data["success"] is True, "接口应返回成功"
    
    # 验证 meal_type 是有效的餐次类型
    valid_meal_types = ["早餐", "午餐", "晚餐", "加餐"]
    assert data["meal_type"] in valid_meal_types, f"meal_type 应为有效的餐次类型，实际值: {data['meal_type']}"
    
    # 验证 message 包含推荐内容
    assert len(data["message"]) > 0, "message 应包含推荐语"
    assert len(data["recommended_dish"]) > 0, "应推荐具体菜式"
    assert len(data["reason"]) > 0, "应包含推荐理由"
    
    # 验证 message 中包含推荐的菜式名称
    assert data["recommended_dish"] in data["message"], "推荐语中应包含推荐的菜式名称"


@pytest.mark.asyncio
async def test_meal_recommend_has_nutrition_highlight(auth_client: AsyncClient):
    """
    测试智能菜式推荐接口 - 验证营养亮点
    """
    response = await auth_client.get("/api/ai/meal/recommend")
    
    assert response.status_code == 200
    
    data = response.json()
    
    # nutrition_highlight 是可选的，但如果存在应该有内容
    if data.get("nutrition_highlight"):
        assert len(data["nutrition_highlight"]) > 0, "营养亮点应有内容"


@pytest.mark.asyncio
async def test_meal_recommend_multiple_calls(auth_client: AsyncClient):
    """
    测试智能菜式推荐接口 - 多次调用稳定性
    
    连续调用两次，验证接口稳定性
    """
    # 第一次调用
    response1 = await auth_client.get("/api/ai/meal/recommend")
    assert response1.status_code == 200
    data1 = response1.json()
    assert data1["success"] is True
    
    # 第二次调用
    response2 = await auth_client.get("/api/ai/meal/recommend")
    assert response2.status_code == 200
    data2 = response2.json()
    assert data2["success"] is True
    
    # 验证两次返回的 meal_type 应该相同（因为时间相近）
    assert data1["meal_type"] == data2["meal_type"], "短时间内多次调用，餐次类型应相同"
