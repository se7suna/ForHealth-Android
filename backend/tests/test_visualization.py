"""
可视化报告 API 测试
测试 Issue #22, #23, #25, #26 的功能
"""
# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
from httpx import AsyncClient
from datetime import date, timedelta
import pytest_asyncio

# ========== Fixtures ==========
from app.config import settings
TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD
}

@pytest_asyncio.fixture
async def async_client():
    """创建异步 HTTP 客户端"""
    async with AsyncClient(base_url="http://127.0.0.1:8000", timeout=30.0, http2=False) as client:
        yield client

@pytest_asyncio.fixture
async def auth_headers(async_client: AsyncClient):
    """获取认证 headers"""
    # 登录获取 token
    response = await async_client.post(
        "/api/auth/login",
        json={
            "email": TEST_USER["email"],
            "password": TEST_USER["password"]
        }
    )
    assert response.status_code == 200, f"登录失败: 状态码={response.status_code}, 响应={response.text}"
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.mark.asyncio
async def test_daily_calorie_summary_default_date(async_client: AsyncClient, auth_headers: dict):
    """测试获取今日卡路里摘要（默认日期）"""
    response = await async_client.get(
        "/api/visualization/daily-calorie-summary",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()

    # 验证返回字段
    assert "date" in data
    assert "total_intake" in data
    assert "total_burned" in data
    assert "daily_goal" in data
    assert "net_calories" in data
    assert "goal_percentage" in data
    assert "is_over_budget" in data

    # 验证数据类型
    assert isinstance(data["total_intake"], (int, float))
    assert isinstance(data["total_burned"], (int, float))
    assert isinstance(data["daily_goal"], (int, float))
    assert isinstance(data["is_over_budget"], bool)


@pytest.mark.asyncio
async def test_daily_calorie_summary_specific_date(async_client: AsyncClient, auth_headers: dict):
    """测试获取指定日期的卡路里摘要"""
    target_date = (date.today() - timedelta(days=1)).isoformat()

    response = await async_client.get(
        f"/api/visualization/daily-calorie-summary?target_date={target_date}",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()
    assert data["date"] == target_date


@pytest.mark.asyncio
async def test_nutrition_analysis(async_client: AsyncClient, auth_headers: dict):
    """测试营养素与食物来源分析"""
    end_date = date.today()
    start_date = end_date - timedelta(days=7)

    response = await async_client.get(
        f"/api/visualization/nutrition-analysis?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()

    # 验证返回字段
    assert "date_range" in data
    assert "macronutrient_ratio" in data
    assert "nutrition_vs_recommended" in data
    assert "food_category_distribution" in data

    # 验证日期范围
    assert data["date_range"]["start_date"] == start_date.isoformat()
    assert data["date_range"]["end_date"] == end_date.isoformat()

    # 验证宏量营养素比例
    macro_ratio = data["macronutrient_ratio"]
    assert "protein" in macro_ratio
    assert "carbohydrates" in macro_ratio
    assert "fat" in macro_ratio

    # 验证营养素推荐量对比
    assert isinstance(data["nutrition_vs_recommended"], list)

    # 验证食物类别分布
    assert isinstance(data["food_category_distribution"], list)


@pytest.mark.asyncio
async def test_nutrition_analysis_invalid_date_range(async_client: AsyncClient, auth_headers: dict):
    """测试无效日期范围（开始日期晚于结束日期）"""
    end_date = date.today() - timedelta(days=7)
    start_date = date.today()

    response = await async_client.get(
        f"/api/visualization/nutrition-analysis?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}",
        headers=auth_headers
    )

    assert response.status_code == 400


@pytest.mark.asyncio
async def test_time_series_trend_day_view(async_client: AsyncClient, auth_headers: dict):
    """测试时间序列趋势分析（日视图）"""
    end_date = date.today()
    start_date = end_date - timedelta(days=7)

    response = await async_client.get(
        f"/api/visualization/time-series-trend?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}&view_type=day",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()

    # 验证返回字段
    assert "view_type" in data
    assert "date_range" in data
    assert "intake_trend" in data
    assert "burned_trend" in data
    assert "weight_trend" in data

    # 验证视图类型
    assert data["view_type"] == "day"

    # 验证趋势数据格式
    assert isinstance(data["intake_trend"], list)
    assert isinstance(data["burned_trend"], list)
    assert isinstance(data["weight_trend"], list)


@pytest.mark.asyncio
async def test_time_series_trend_week_view(async_client: AsyncClient, auth_headers: dict):
    """测试时间序列趋势分析（周视图）"""
    end_date = date.today()
    start_date = end_date - timedelta(days=30)

    response = await async_client.get(
        f"/api/visualization/time-series-trend?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}&view_type=week",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()
    assert data["view_type"] == "week"


@pytest.mark.asyncio
async def test_time_series_trend_month_view(async_client: AsyncClient, auth_headers: dict):
    """测试时间序列趋势分析（月视图）"""
    end_date = date.today()
    start_date = end_date - timedelta(days=90)

    response = await async_client.get(
        f"/api/visualization/time-series-trend?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}&view_type=month",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()
    assert data["view_type"] == "month"


@pytest.mark.asyncio
async def test_time_series_trend_invalid_view_type(async_client: AsyncClient, auth_headers: dict):
    """测试无效的视图类型"""
    end_date = date.today()
    start_date = end_date - timedelta(days=7)

    response = await async_client.get(
        f"/api/visualization/time-series-trend?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}&view_type=invalid",
        headers=auth_headers
    )

    assert response.status_code == 400


@pytest.mark.asyncio
async def test_export_report(async_client: AsyncClient, auth_headers: dict):
    """测试导出健康数据报告"""
    end_date = date.today()
    start_date = end_date - timedelta(days=7)

    response = await async_client.get(
        f"/api/visualization/export-report?start_date={start_date.isoformat()}&end_date={end_date.isoformat()}",
        headers=auth_headers
    )

    assert response.status_code == 200
    data = response.json()

    # 验证返回字段
    assert "user_info" in data
    assert "date_range" in data
    assert "summary" in data
    assert "daily_calorie_summary" in data
    assert "nutrition_analysis" in data
    assert "time_series_trend" in data
    assert "generated_at" in data

    # 验证用户信息
    user_info = data["user_info"]
    assert "username" in user_info
    assert "email" in user_info

    # 验证摘要信息
    summary = data["summary"]
    assert "days_count" in summary
    assert "total_food_records" in summary
    assert "total_sports_records" in summary
    assert "total_intake_calories" in summary
    assert "total_burned_calories" in summary
    assert "average_daily_intake" in summary
    assert "average_daily_burned" in summary

    # 验证天数计算正确
    expected_days = (end_date - start_date).days + 1
    assert summary["days_count"] == expected_days


@pytest.mark.asyncio
async def test_visualization_requires_authentication(async_client: AsyncClient):
    """测试可视化 API 需要认证"""
    endpoints = [
        "/api/visualization/daily-calorie-summary",
        "/api/visualization/nutrition-analysis?start_date=2025-11-01&end_date=2025-11-23",
        "/api/visualization/time-series-trend?start_date=2025-11-01&end_date=2025-11-23",
        "/api/visualization/export-report?start_date=2025-11-01&end_date=2025-11-23"
    ]

    for endpoint in endpoints:
        response = await async_client.get(endpoint)
        assert response.status_code == 401, f"Endpoint {endpoint} should require authentication"
