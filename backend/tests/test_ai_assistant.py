"""
AI助手模块测试

测试覆盖:
1. 拍照识别食物:图片识别、确认并添加到日志
2. 生成个性化饮食计划:生成计划、验证营养配比
3. 营养知识问答:问答功能、上下文理解
4. 智能提醒与反馈:提醒设置、通知管理、每日反馈

注意: AI 功能路由尚未实现,所有测试暂时跳过
"""

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from httpx import AsyncClient
from datetime import date, datetime
import pytest_asyncio
import io

# 跳过整个测试模块 - AI 路由尚未实现
pytestmark = pytest.mark.skip(reason="AI 功能路由尚未实现,等待后续开发")

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
    """示例食物数据 - 用于测试"""
    return {
        "name": "测试苹果_AI",
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
        }
    }


@pytest.fixture
def sample_image_file():
    """创建示例图片文件（用于测试图片上传）"""
    # 尝试使用测试目录中的真实图片
    from pathlib import Path
    test_image_path = Path(__file__).parent / "test_picture" / "image2.png"
    
    if test_image_path.exists():
        # 使用真实测试图片
        with open(test_image_path, "rb") as f:
            image_data = f.read()
        return ("image1.png", io.BytesIO(image_data), "image/png")
    else:
        # 创建一个简单的测试图片（1x1像素的PNG）
        image_data = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xdb\x00\x00\x00\x00IEND\xaeB`\x82'
        return ("test_image.png", io.BytesIO(image_data), "image/png")


# ========== 拍照识别食物测试 ==========

@pytest.mark.asyncio
async def test_food_image_recognition_success(auth_client, sample_image_file):
    """测试食物图片识别 - 成功"""
    expected_success = {
        "status_code": 200,
        "has_recognized_foods": True
    }
    
    # 上传图片进行识别
    # 注意：sample_image_file[1] 是 BytesIO 对象，需要读取内容
    image_name, image_io, image_type = sample_image_file
    image_data = image_io.read()
    image_io.seek(0)  # 重置指针以便后续使用
    
    files = {
        "file": (image_name, image_data, image_type)
    }
    
    response = await auth_client.post(
        "/api/ai/food/recognize-image",
        files=files
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "recognized_foods" in data
        assert isinstance(data["recognized_foods"], list)
        if data.get("success"):
            assert len(data["recognized_foods"]) > 0 or "message" in data


@pytest.mark.asyncio
async def test_food_image_recognition_invalid_file(auth_client):
    """测试食物图片识别 - 无效文件"""
    expected_error = {
        "status_code": 400
    }
    
    # 上传非图片文件
    files = {
        "file": ("test.txt", b"not an image", "text/plain")
    }
    
    response = await auth_client.post(
        "/api/ai/food/recognize-image",
        files=files
    )
    
    # 应该返回错误状态码
    assert response.status_code in [400, 422]


@pytest.mark.asyncio
async def test_food_recognition_confirm(auth_client, sample_food_data):
    """测试确认识别结果并添加到饮食日志"""
    expected_success = {
        "status_code": 200,
        "has_created_records": True
    }
    
    # 先创建一个测试食物
    food_response = await auth_client.post("/api/food/", json=sample_food_data)
    if food_response.status_code != 201:
        pytest.skip("无法创建测试食物")
    
    food_id = food_response.json().get("id")
    
    # 确认识别结果
    confirm_data = {
        "recognized_foods": [
            {
                "food_name": sample_food_data["name"],
                "serving_size": 150,
                "serving_unit": "克",
                "nutrition_per_serving": sample_food_data["nutrition_per_serving"],
                "food_id": food_id,
                "source": "database"
            }
        ],
        "recorded_at": datetime.now().isoformat(),
        "meal_type": "午餐",
        "notes": "AI识别后确认"
    }
    
    response = await auth_client.post(
        "/api/ai/food/confirm-recognition",
        json=confirm_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "created_records" in data
        assert isinstance(data["created_records"], list)
        
        # 清理：删除创建的食物记录
        if data.get("created_records"):
            for record_id in data["created_records"]:
                await auth_client.delete(f"/api/food/record/{record_id}")
    
    # 清理：删除测试食物
    if food_id:
        await auth_client.delete(f"/api/food/{food_id}")


@pytest.mark.asyncio
async def test_food_recognition_confirm_invalid_data(auth_client):
    """测试确认识别结果 - 无效数据"""
    expected_error = {
        "status_code": 422
    }
    
    # 缺少必填字段
    confirm_data = {
        "recognized_foods": [],
        "recorded_at": datetime.now().isoformat()
    }
    
    response = await auth_client.post(
        "/api/ai/food/confirm-recognition",
        json=confirm_data
    )
    
    # 应该返回验证错误
    assert response.status_code in [400, 422]


# ========== 生成个性化饮食计划测试 ==========

@pytest.mark.asyncio
async def test_generate_meal_plan_week(auth_client):
    """测试生成一周饮食计划"""
    expected_success = {
        "status_code": 200,
        "has_daily_plans": True
    }
    
    meal_plan_data = {
        "plan_duration": "week",
        "include_budget": False,
        "food_preference": {
            "liked_foods": ["苹果", "鸡胸肉", "西兰花"],
            "disliked_foods": ["茄子"],
            "dietary_restrictions": ["低钠"],
            "preferred_tastes": ["清淡"]
        },
        "meals_per_day": 3
    }
    
    response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=meal_plan_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "daily_plans" in data
        assert isinstance(data["daily_plans"], list)
        if data.get("success"):
            assert len(data["daily_plans"]) > 0
            # 验证每日计划结构
            daily_plan = data["daily_plans"][0]
            assert "date" in daily_plan
            assert "meals" in daily_plan
            assert "daily_nutrition" in daily_plan


@pytest.mark.asyncio
async def test_generate_meal_plan_with_budget(auth_client):
    """测试生成带预算的饮食计划"""
    expected_success = {
        "status_code": 200
    }
    
    meal_plan_data = {
        "plan_duration": "day",
        "plan_days": 1,
        "include_budget": True,
        "budget_per_day": 50,
        "target_calories": 1800,
        "meals_per_day": 3
    }
    
    response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=meal_plan_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        if data.get("success"):
            # 验证预算相关字段
            assert "total_cost" in data or "average_daily_cost" in data


@pytest.mark.asyncio
async def test_generate_meal_plan_invalid_duration(auth_client):
    """测试生成饮食计划 - 无效的计划时间"""
    expected_error = {
        "status_code": 422
    }
    
    meal_plan_data = {
        "plan_duration": "invalid",
        "meals_per_day": 3
    }
    
    response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=meal_plan_data
    )
    
    # 应该返回验证错误
    assert response.status_code in [400, 422]


@pytest.mark.asyncio
async def test_generate_meal_plan_missing_plan_days(auth_client):
    """测试生成饮食计划 - 缺少plan_days"""
    expected_error = {
        "status_code": 422
    }
    
    meal_plan_data = {
        "plan_duration": "day",
        # 缺少 plan_days
        "meals_per_day": 3
    }
    
    response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=meal_plan_data
    )
    
    # 应该返回验证错误
    assert response.status_code in [400, 422]


# ========== 营养知识问答测试 ==========

@pytest.mark.asyncio
async def test_nutrition_question_success(auth_client):
    """测试营养知识问答 - 成功"""
    expected_success = {
        "status_code": 200,
        "has_answer": True
    }
    
    question_data = {
        "question": "蛋白质补充的最佳时间是什么时候？"
    }
    
    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=question_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "question" in data
        assert "answer" in data
        if data.get("success"):
            assert len(data["answer"]) > 0


@pytest.mark.asyncio
async def test_nutrition_question_with_context(auth_client):
    """测试营养知识问答 - 带上下文"""
    expected_success = {
        "status_code": 200
    }
    
    question_data = {
        "question": "我应该每天摄入多少蛋白质？",
        "context": {
            "user_goal": "增肌",
            "activity_level": "high",
            "weight": 70
        }
    }
    
    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=question_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "answer" in data


@pytest.mark.asyncio
async def test_nutrition_question_empty_question(auth_client):
    """测试营养知识问答 - 空问题"""
    expected_error = {
        "status_code": 422
    }
    
    question_data = {
        "question": ""
    }
    
    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=question_data
    )
    
    # 应该返回验证错误
    assert response.status_code in [400, 422]


@pytest.mark.asyncio
async def test_nutrition_question_long_question(auth_client):
    """测试营养知识问答 - 问题过长"""
    expected_error = {
        "status_code": 422
    }
    
    question_data = {
        "question": "A" * 501  # 超过500字符限制
    }
    
    response = await auth_client.post(
        "/api/ai/nutrition/ask",
        json=question_data
    )
    
    # 应该返回验证错误
    assert response.status_code in [400, 422]


# ========== 智能提醒与反馈测试 ==========

@pytest.mark.asyncio
async def test_get_reminder_settings(auth_client):
    """测试获取提醒设置"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/ai/reminders/settings")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "settings" in data
        settings = data["settings"]
        assert "meal_reminders" in settings
        assert "record_reminders" in settings


@pytest.mark.asyncio
async def test_update_reminder_settings(auth_client):
    """测试更新提醒设置"""
    expected_success = {
        "status_code": 200
    }
    
    settings_data = {
        "settings": {
            "meal_reminders": True,
            "meal_reminder_times": ["07:00", "12:00", "18:00"],
            "record_reminders": True,
            "record_reminder_hours": 3,
            "goal_reminders": True,
            "motivational_messages": True
        }
    }
    
    response = await auth_client.put(
        "/api/ai/reminders/settings",
        json=settings_data
    )
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "settings" in data
        updated_settings = data["settings"]
        assert updated_settings["meal_reminders"] == settings_data["settings"]["meal_reminders"]
        assert updated_settings["meal_reminder_times"] == settings_data["settings"]["meal_reminder_times"]


@pytest.mark.asyncio
async def test_get_notifications(auth_client):
    """测试获取通知列表"""
    expected_success = {
        "status_code": 200
    }
    
    response = await auth_client.get("/api/ai/notifications")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "total" in data
        assert "unread_count" in data
        assert "notifications" in data
        assert isinstance(data["notifications"], list)


@pytest.mark.asyncio
async def test_get_notifications_with_params(auth_client):
    """测试获取通知列表 - 带参数"""
    expected_success = {
        "status_code": 200
    }
    
    # 测试分页和筛选
    response = await auth_client.get("/api/ai/notifications?limit=10&offset=0&unread_only=true")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "notifications" in data
        assert isinstance(data["notifications"], list)


@pytest.mark.asyncio
async def test_mark_notifications_read(auth_client):
    """测试标记通知为已读"""
    expected_success = {
        "status_code": 200
    }
    
    # 先获取通知列表
    notifications_response = await auth_client.get("/api/ai/notifications")
    if notifications_response.status_code == 200:
        notifications_data = notifications_response.json()
        if notifications_data.get("notifications"):
            # 获取第一个未读通知的ID
            unread_notifications = [n for n in notifications_data["notifications"] if not n.get("read")]
            if unread_notifications:
                notification_ids = [unread_notifications[0]["id"]]
                
                read_data = {
                    "notification_ids": notification_ids
                }
                
                response = await auth_client.post(
                    "/api/ai/notifications/mark-read",
                    json=read_data
                )
                
                assert response.status_code == expected_success["status_code"]
                if response.status_code == 200:
                    data = response.json()
                    assert "success" in data
            else:
                pytest.skip("没有未读通知可供测试")
        else:
            pytest.skip("没有通知可供测试")
    else:
        pytest.skip("无法获取通知列表")


@pytest.mark.asyncio
async def test_get_daily_feedback(auth_client):
    """测试获取每日反馈"""
    expected_success = {
        "status_code": 200
    }
    
    target_date = date.today()
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{target_date}")
    
    assert response.status_code == expected_success["status_code"]
    if response.status_code == 200:
        data = response.json()
        assert "success" in data
        assert "feedback" in data
        feedback = data["feedback"]
        assert "date" in feedback
        assert "daily_calories" in feedback
        assert "target_calories" in feedback
        assert "nutrition_summary" in feedback
        assert "goal_status" in feedback


@pytest.mark.asyncio
async def test_get_daily_feedback_invalid_date(auth_client):
    """测试获取每日反馈 - 无效日期"""
    expected_error = {
        "status_code": 422
    }
    
    # 使用无效日期格式
    response = await auth_client.get("/api/ai/feedback/daily/invalid-date")
    
    # 应该返回错误
    assert response.status_code in [400, 422, 404]


@pytest.mark.asyncio
async def test_get_daily_feedback_future_date(auth_client):
    """测试获取每日反馈 - 未来日期"""
    # 未来日期可能返回空数据或错误
    from datetime import timedelta
    future_date = date.today() + timedelta(days=7)
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{future_date}")
    
    # 未来日期可能返回200（空数据）或400/422（错误）
    assert response.status_code in [200, 400, 422]


# ========== 集成测试 ==========

@pytest.mark.asyncio
async def test_ai_workflow_complete(auth_client, sample_food_data):
    """测试完整的AI助手工作流程"""
    """
    测试流程：
    1. 生成饮食计划
    2. 获取每日反馈
    3. 查看通知
    4. 更新提醒设置
    """
    
    # 1. 生成饮食计划
    meal_plan_data = {
        "plan_duration": "day",
        "plan_days": 1,
        "meals_per_day": 3
    }
    
    plan_response = await auth_client.post(
        "/api/ai/meal-plan/generate",
        json=meal_plan_data
    )
    assert plan_response.status_code == 200
    
    # 2. 获取今日反馈
    today = date.today()
    feedback_response = await auth_client.get(f"/api/ai/feedback/daily/{today}")
    assert feedback_response.status_code == 200
    
    # 3. 查看通知
    notifications_response = await auth_client.get("/api/ai/notifications")
    assert notifications_response.status_code == 200
    
    # 4. 更新提醒设置
    settings_data = {
        "settings": {
            "meal_reminders": True,
            "meal_reminder_times": ["07:00", "12:00", "18:00"],
            "record_reminders": True,
            "record_reminder_hours": 3,
            "goal_reminders": True,
            "motivational_messages": True
        }
    }
    
    settings_response = await auth_client.put(
        "/api/ai/reminders/settings",
        json=settings_data
    )
    assert settings_response.status_code == 200

