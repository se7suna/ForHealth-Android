"""
智能提醒与反馈功能测试

测试覆盖：
1. 提醒设置管理（获取、更新）
2. 通知管理（获取列表、标记已读）
3. 每日反馈（获取反馈数据、生成建议）

注意：每日反馈中的个性化建议需要调用 LLM，可能需要 60 秒左右。
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
import pytest_asyncio
from httpx import AsyncClient
from datetime import date, timedelta

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


# ========== 提醒设置测试 ==========

@pytest.mark.asyncio
async def test_get_reminder_settings(auth_client: AsyncClient):
    """
    测试获取提醒设置
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回默认设置（如果用户未设置）
    3. 返回数据结构符合预期
    """
    response = await auth_client.get("/api/ai/reminders/settings")

    assert (
        response.status_code == 200
    ), f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    assert "success" in data
    assert data["success"] is True
    assert "message" in data
    assert "settings" in data
    
    settings = data["settings"]
    assert "meal_reminders" in settings
    assert "record_reminders" in settings
    assert "goal_reminders" in settings
    assert "motivational_messages" in settings
    assert isinstance(settings["meal_reminders"], bool)
    assert isinstance(settings["record_reminders"], bool)


@pytest.mark.asyncio
async def test_update_reminder_settings(auth_client: AsyncClient):
    """
    测试更新提醒设置
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 设置能成功保存
    3. 更新后的设置与请求一致
    """
    settings_data = {
        "settings": {
            "meal_reminders": True,
            "meal_reminder_times": ["07:00", "12:00", "18:00"],
            "record_reminders": True,
            "record_reminder_hours": 3,
            "goal_reminders": True,
            "motivational_messages": True,
        }
    }

    response = await auth_client.put(
        "/api/ai/reminders/settings",
        json=settings_data
    )

    assert (
        response.status_code == 200
    ), f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    assert "success" in data
    assert data["success"] is True
    assert "settings" in data
    
    updated_settings = data["settings"]
    assert updated_settings["meal_reminders"] == settings_data["settings"]["meal_reminders"]
    assert updated_settings["meal_reminder_times"] == settings_data["settings"]["meal_reminder_times"]
    assert updated_settings["record_reminder_hours"] == settings_data["settings"]["record_reminder_hours"]


@pytest.mark.asyncio
async def test_update_reminder_settings_partial(auth_client: AsyncClient):
    """
    测试部分更新提醒设置
    
    测试要点：
    1. 只更新部分设置项
    2. 其他设置项保持原值或使用默认值
    """
    settings_data = {
        "settings": {
            "meal_reminders": False,
            "meal_reminder_times": ["08:00", "13:00"],
            "record_reminders": False,
        }
    }

    response = await auth_client.put(
        "/api/ai/reminders/settings",
        json=settings_data
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["settings"]["meal_reminders"] is False
    assert data["settings"]["record_reminders"] is False


@pytest.mark.asyncio
async def test_update_reminder_settings_invalid_time_format(auth_client: AsyncClient):
    """
    测试更新提醒设置 - 无效时间格式
    
    测试要点：
    1. 无效的时间格式应该被拒绝或返回错误
    2. 时间格式必须是 HH:MM
    """
    settings_data = {
        "settings": {
            "meal_reminders": True,
            "meal_reminder_times": ["7:00", "25:00", "invalid"],  # 无效格式
            "record_reminders": True,
        }
    }

    response = await auth_client.put(
        "/api/ai/reminders/settings",
        json=settings_data
    )

    # 可能返回 200（如果后端做了验证和清理）或 422（验证错误）
    assert response.status_code in [200, 422]


# ========== 通知管理测试 ==========

@pytest.mark.asyncio
async def test_get_notifications(auth_client: AsyncClient):
    """
    测试获取通知列表
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 包含总数和未读数
    """
    response = await auth_client.get("/api/ai/notifications")

    assert (
        response.status_code == 200
    ), f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    assert "total" in data
    assert "unread_count" in data
    assert "notifications" in data
    assert isinstance(data["notifications"], list)
    assert isinstance(data["total"], int)
    assert isinstance(data["unread_count"], int)
    assert data["total"] >= 0
    assert data["unread_count"] >= 0
    assert data["unread_count"] <= data["total"]


@pytest.mark.asyncio
async def test_get_notifications_with_pagination(auth_client: AsyncClient):
    """
    测试获取通知列表 - 分页
    
    测试要点：
    1. limit 参数限制返回数量
    2. offset 参数实现分页
    """
    # 测试第一页
    response = await auth_client.get("/api/ai/notifications?limit=10&offset=0")
    assert response.status_code == 200
    data = response.json()
    assert len(data["notifications"]) <= 10
    
    # 测试第二页
    if data["total"] > 10:
        response2 = await auth_client.get("/api/ai/notifications?limit=10&offset=10")
        assert response2.status_code == 200
        data2 = response2.json()
        assert len(data2["notifications"]) <= 10


@pytest.mark.asyncio
async def test_get_notifications_unread_only(auth_client: AsyncClient):
    """
    测试获取通知列表 - 只返回未读
    
    测试要点：
    1. unread_only=true 时只返回未读通知
    2. 所有返回的通知 read 字段应为 False
    """
    response = await auth_client.get("/api/ai/notifications?unread_only=true")

    assert response.status_code == 200
    data = response.json()
    
    # 验证所有通知都是未读
    for notification in data["notifications"]:
        assert notification["read"] is False


@pytest.mark.asyncio
async def test_mark_notifications_read(auth_client: AsyncClient):
    """
    测试标记通知为已读
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 通知被成功标记为已读
    3. 再次获取时该通知的 read 字段为 True
    
    注意：如果当前没有通知，会先调用每日反馈接口尝试生成通知
    """
    # 先尝试获取通知列表
    notifications_response = await auth_client.get("/api/ai/notifications")
    assert notifications_response.status_code == 200
    
    notifications_data = notifications_response.json()
    
    # 如果没有通知，尝试通过获取每日反馈来生成通知
    if not notifications_data.get("notifications"):
        # 调用每日反馈接口，可能会生成通知
        today = date.today()
        feedback_response = await auth_client.get(f"/api/ai/feedback/daily/{today}")
        # 无论是否生成通知，都继续测试
        
        # 再次获取通知列表
        notifications_response = await auth_client.get("/api/ai/notifications")
        assert notifications_response.status_code == 200
        notifications_data = notifications_response.json()
    
    # 如果仍然没有通知，测试标记空列表的情况
    if not notifications_data.get("notifications"):
        # 测试标记空列表（应该返回成功，但更新数量为0）
        read_data = {
            "notification_ids": ["nonexistent_id_12345"]
        }
        response = await auth_client.post(
            "/api/ai/notifications/mark-read",
            json=read_data
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["updated_count"] == 0
        return
    
    # 获取第一个未读通知的ID
    unread_notifications = [
        n for n in notifications_data["notifications"] 
        if not n.get("read")
    ]
    
    # 如果没有未读通知，使用已读通知测试（应该也能正常处理）
    if not unread_notifications:
        # 使用第一个通知（即使是已读的）
        notification_id = notifications_data["notifications"][0]["id"]
    else:
        notification_id = unread_notifications[0]["id"]
    
    # 标记为已读
    read_data = {
        "notification_ids": [notification_id]
    }
    
    response = await auth_client.post(
        "/api/ai/notifications/mark-read",
        json=read_data
    )
    
    assert (
        response.status_code == 200
    ), f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    assert "success" in data
    assert data["success"] is True
    assert "updated_count" in data
    assert data["updated_count"] >= 0
    
    # 验证通知已被标记为已读
    notifications_response2 = await auth_client.get("/api/ai/notifications")
    assert notifications_response2.status_code == 200
    notifications_data2 = notifications_response2.json()
    
    # 查找该通知
    found_notification = None
    for n in notifications_data2["notifications"]:
        if n["id"] == notification_id:
            found_notification = n
            break
    
    if found_notification:
        assert found_notification["read"] is True


@pytest.mark.asyncio
async def test_mark_multiple_notifications_read(auth_client: AsyncClient):
    """
    测试标记多个通知为已读
    
    测试要点：
    1. 可以一次标记多个通知
    2. 返回更新的通知数量
    
    注意：如果通知不足，会测试标记部分通知或空列表的情况
    """
    # 先尝试获取通知列表
    notifications_response = await auth_client.get("/api/ai/notifications")
    assert notifications_response.status_code == 200
    
    notifications_data = notifications_response.json()
    
    # 如果没有通知，尝试通过获取每日反馈来生成通知
    if not notifications_data.get("notifications"):
        today = date.today()
        await auth_client.get(f"/api/ai/feedback/daily/{today}")
        notifications_response = await auth_client.get("/api/ai/notifications")
        assert notifications_response.status_code == 200
        notifications_data = notifications_response.json()
    
    # 获取所有通知（包括已读和未读）
    all_notifications = notifications_data.get("notifications", [])
    
    if len(all_notifications) == 0:
        # 测试标记空列表
        read_data = {
            "notification_ids": ["nonexistent_id_1", "nonexistent_id_2"]
        }
        response = await auth_client.post(
            "/api/ai/notifications/mark-read",
            json=read_data
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["updated_count"] == 0
        return
    
    # 获取前两个通知的ID（无论是否已读）
    notification_ids = [n["id"] for n in all_notifications[:2]]
    
    # 标记为已读
    read_data = {
        "notification_ids": notification_ids
    }
    
    response = await auth_client.post(
        "/api/ai/notifications/mark-read",
        json=read_data
    )
    
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["updated_count"] >= 0
    
    # 如果只有1个通知，updated_count 可能是 0 或 1（取决于是否已读）
    # 如果有2个通知，updated_count 可能是 0, 1, 或 2
    assert 0 <= data["updated_count"] <= len(notification_ids)


# ========== 每日反馈测试 ==========

@pytest.mark.asyncio
async def test_get_daily_feedback_today(auth_client: AsyncClient):
    """
    测试获取今日反馈
    
    测试要点：
    1. 接口能正常调用并返回 200
    2. 返回数据结构符合预期
    3. 包含营养汇总、目标进度、建议等
    """
    today = date.today()
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{today}")

    assert (
        response.status_code == 200
    ), f"请求失败: 状态码={response.status_code}, 响应={response.text}"
    
    data = response.json()
    assert "success" in data
    assert data["success"] is True
    assert "feedback" in data
    
    feedback = data["feedback"]
    assert "date" in feedback
    assert "daily_calories" in feedback
    assert "target_calories" in feedback
    assert "calories_progress" in feedback
    assert "nutrition_summary" in feedback
    assert "meal_count" in feedback
    assert "goal_status" in feedback
    assert "suggestions" in feedback
    
    # 验证数据类型
    assert isinstance(feedback["daily_calories"], (int, float))
    assert isinstance(feedback["target_calories"], (int, float))
    assert isinstance(feedback["calories_progress"], (int, float))
    assert 0 <= feedback["calories_progress"] <= 1
    assert isinstance(feedback["meal_count"], int)
    assert feedback["goal_status"] in ["on_track", "exceeded", "below"]
    assert isinstance(feedback["suggestions"], list)
    
    # 验证营养摘要
    nutrition_summary = feedback["nutrition_summary"]
    assert "calories" in nutrition_summary
    assert "protein" in nutrition_summary
    assert "carbohydrates" in nutrition_summary
    assert "fat" in nutrition_summary


@pytest.mark.asyncio
async def test_get_daily_feedback_yesterday(auth_client: AsyncClient):
    """
    测试获取昨日反馈
    
    测试要点：
    1. 可以获取历史日期的反馈
    2. 如果该日期没有记录，返回空数据
    """
    yesterday = date.today() - timedelta(days=1)
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{yesterday}")

    assert response.status_code == 200
    data = response.json()
    assert "success" in data
    assert "feedback" in data
    
    feedback = data["feedback"]
    assert feedback["date"] == yesterday.strftime("%Y-%m-%d")
    assert feedback["daily_calories"] >= 0


@pytest.mark.asyncio
async def test_get_daily_feedback_future_date(auth_client: AsyncClient):
    """
    测试获取未来日期反馈
    
    测试要点：
    1. 未来日期应该返回空数据或错误
    2. 不应该有营养记录
    """
    future_date = date.today() + timedelta(days=7)
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{future_date}")

    # 未来日期可能返回 200（空数据）或 400/422（错误）
    assert response.status_code in [200, 400, 422]
    
    if response.status_code == 200:
        data = response.json()
        assert "feedback" in data
        feedback = data["feedback"]
        # 未来日期应该没有记录
        assert feedback["daily_calories"] == 0
        assert feedback["meal_count"] == 0


@pytest.mark.asyncio
async def test_get_daily_feedback_invalid_date(auth_client: AsyncClient):
    """
    测试获取反馈 - 无效日期格式
    
    测试要点：
    1. 无效日期格式应该返回 422 错误
    """
    response = await auth_client.get("/api/ai/feedback/daily/invalid-date")

    # 应该返回 422（验证错误）或 400（错误请求）
    assert response.status_code in [400, 422]


@pytest.mark.asyncio
async def test_get_daily_feedback_with_notification(auth_client: AsyncClient):
    """
    测试获取反馈时生成通知
    
    测试要点：
    1. 如果满足条件（如超标或达成目标），会自动生成通知
    2. 通知包含在响应中
    """
    today = date.today()
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{today}")

    assert response.status_code == 200
    data = response.json()
    
    # notification 字段是可选的
    if "notification" in data and data["notification"]:
        notification = data["notification"]
        assert "id" in notification
        assert "type" in notification
        assert "title" in notification
        assert "content" in notification
        assert "created_at" in notification


# ========== 集成测试 ==========

@pytest.mark.asyncio
async def test_reminder_workflow_complete(auth_client: AsyncClient):
    """
    测试完整的提醒功能工作流程
    
    测试流程：
    1. 获取默认提醒设置
    2. 更新提醒设置
    3. 获取今日反馈（可能生成通知）
    4. 查看通知列表
    5. 标记通知为已读
    """
    # 1. 获取提醒设置
    response1 = await auth_client.get("/api/ai/reminders/settings")
    assert response1.status_code == 200
    settings1 = response1.json()["settings"]
    
    # 2. 更新提醒设置
    new_settings = {
        "settings": {
            "meal_reminders": True,
            "meal_reminder_times": ["08:00", "13:00", "19:00"],
            "record_reminders": True,
            "record_reminder_hours": 4,
            "goal_reminders": True,
            "motivational_messages": True,
        }
    }
    response2 = await auth_client.put(
        "/api/ai/reminders/settings",
        json=new_settings
    )
    assert response2.status_code == 200
    updated_settings = response2.json()["settings"]
    assert updated_settings["meal_reminder_times"] == new_settings["settings"]["meal_reminder_times"]
    
    # 3. 获取今日反馈
    today = date.today()
    response3 = await auth_client.get(f"/api/ai/feedback/daily/{today}")
    assert response3.status_code == 200
    feedback_data = response3.json()
    assert "feedback" in feedback_data
    
    # 4. 查看通知列表
    response4 = await auth_client.get("/api/ai/notifications")
    assert response4.status_code == 200
    notifications_data = response4.json()
    assert "notifications" in notifications_data
    
    # 5. 如果有未读通知，标记为已读
    unread_notifications = [
        n for n in notifications_data["notifications"]
        if not n.get("read")
    ]
    
    if unread_notifications:
        notification_id = unread_notifications[0]["id"]
        response5 = await auth_client.post(
            "/api/ai/notifications/mark-read",
            json={"notification_ids": [notification_id]}
        )
        assert response5.status_code == 200
        assert response5.json()["success"] is True


@pytest.mark.asyncio
async def test_feedback_suggestions_format(auth_client: AsyncClient):
    """
    测试反馈建议的格式
    
    测试要点：
    1. 建议是字符串列表
    2. 建议数量合理（通常 3-5 条）
    3. 建议内容不为空
    """
    today = date.today()
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{today}")
    
    assert response.status_code == 200
    data = response.json()
    feedback = data["feedback"]
    
    suggestions = feedback["suggestions"]
    assert isinstance(suggestions, list)
    assert len(suggestions) > 0  # 至少有一条建议
    assert len(suggestions) <= 5  # 最多 5 条建议
    
    # 验证每条建议都是非空字符串
    for suggestion in suggestions:
        assert isinstance(suggestion, str)
        assert len(suggestion.strip()) > 0


@pytest.mark.asyncio
async def test_feedback_goal_status_calculation(auth_client: AsyncClient):
    """
    测试反馈目标状态的计算
    
    测试要点：
    1. 根据完成进度正确计算目标状态
    2. 状态值符合预期（on_track、exceeded、below）
    """
    today = date.today()
    
    response = await auth_client.get(f"/api/ai/feedback/daily/{today}")
    
    assert response.status_code == 200
    data = response.json()
    feedback = data["feedback"]
    
    progress = feedback["calories_progress"]
    status = feedback["goal_status"]
    
    # 验证状态与进度的一致性
    if progress > 1.1:
        assert status == "exceeded"
    elif progress < 0.8:
        assert status == "below"
    else:
        assert status == "on_track"

