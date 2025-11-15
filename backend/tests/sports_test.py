# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
from httpx import AsyncClient
from datetime import datetime, date, timedelta
from unittest.mock import AsyncMock, patch, MagicMock
from bson import ObjectId

from app.main import app
from app.schemas.sports import (
    LogSportsRequest,
    UpdateSportsRecordRequest,
    CreateSportsRequest,
    UpdateSportsRequest,
    SearchSportRecordsRequest
)


# ==================== 测试Fixtures ====================

@pytest.fixture
def mock_current_user():
    """模拟当前用户"""
    return "test@example.com"


@pytest.fixture
def mock_auth_dependency(mock_current_user):
    """模拟认证依赖"""
    async def override_get_current_user():
        return mock_current_user
    return override_get_current_user


@pytest.fixture
def mock_database():
    """模拟数据库"""
    db = MagicMock()

    # 模拟 sports 集合
    sports_collection = MagicMock()
    sports_collection.find_one = AsyncMock()
    sports_collection.insert_one = AsyncMock()
    sports_collection.find_one_and_update = AsyncMock()
    sports_collection.delete_one = AsyncMock()
    sports_collection.find = MagicMock()

    # 模拟 sports_log 集合
    sports_log_collection = MagicMock()
    sports_log_collection.find_one = AsyncMock()
    sports_log_collection.insert_one = AsyncMock()
    sports_log_collection.find_one_and_update = AsyncMock()
    sports_log_collection.delete_one = AsyncMock()
    sports_log_collection.find = MagicMock()

    db.__getitem__ = lambda self, key: sports_collection if key == "sports" else sports_log_collection

    return db


@pytest.fixture
def sample_sport_type():
    """示例运动类型数据"""
    return {
        "sport_type": "篮球",
        "describe": "团队运动",
        "METs": 6.5,
        "email": "test@example.com"
    }


@pytest.fixture
def sample_sport_record():
    """示例运动记录数据"""
    return {
        "_id": ObjectId(),
        "email": "test@example.com",
        "sport_type": "跑步",
        "created_at": datetime.utcnow(),
        "duration_time": 30,
        "calories_burned": 300.0
    }


# ==================== Router 测试 ====================

class TestSportsRouters:
    """测试运动相关的API路由"""

    @pytest.mark.asyncio
    async def test_create_sports_success(self, mock_auth_dependency, mock_database):
        """测试成功创建自定义运动类型"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = None
            mock_result = MagicMock()
            mock_result.inserted_id = ObjectId()
            mock_database["sports"].insert_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/create-sport",
                    json={
                        "sport_type": "篮球",
                        "describe": "团队运动",
                        "METs": 6.5
                    }
                )

                assert response.status_code == 200
                assert response.json()["success"] == True
                assert "创建" in response.json()["message"]

    @pytest.mark.asyncio
    async def test_create_sports_duplicate(self, mock_auth_dependency, mock_database):
        """测试创建重复的运动类型"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = {"sport_type": "篮球"}

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/create-sport",
                    json={
                        "sport_type": "篮球",
                        "describe": "团队运动",
                        "METs": 6.5
                    }
                )

                assert response.status_code == 400
                assert "已存在" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_get_available_sports_types(self, mock_auth_dependency, mock_database):
        """测试获取可用运动类型列表"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=[
                {"sport_type": "跑步", "describe": "有氧运动", "METs": 8.0},
                {"sport_type": "篮球", "describe": "团队运动", "METs": 6.5}
            ])
            mock_database["sports"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/get-available-sports-types")

                assert response.status_code == 200
                assert len(response.json()) == 2
                assert response.json()[0]["sport_type"] == "跑步"

    @pytest.mark.asyncio
    async def test_delete_sports_success(self, mock_auth_dependency, mock_database):
        """测试成功删除自定义运动类型"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_result = MagicMock()
            mock_result.deleted_count = 1
            mock_database["sports"].delete_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/delete-sport/篮球")

                assert response.status_code == 200
                assert response.json()["success"] == True

    @pytest.mark.asyncio
    async def test_log_sports_record_success(self, mock_auth_dependency, mock_database):
        """测试成功记录运动"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            # 模拟运动类型存在
            mock_database["sports"].find_one.return_value = {
                "sport_type": "跑步",
                "METs": 8.0,
                "email": "sport@default.com"
            }

            # 模拟用户体重
            with patch("app.services.sports_service.get_user_weight", return_value=70.0):
                mock_result = MagicMock()
                mock_result.inserted_id = ObjectId()
                mock_database["sports_log"].insert_one.return_value = mock_result

                async with AsyncClient(app=app, base_url="http://test") as client:
                    response = await client.post(
                        "/sports/log-sports",
                        json={
                            "sport_type": "跑步",
                            "duration_time": 30,
                            "created_at": datetime.utcnow().isoformat()
                        }
                    )

                    assert response.status_code == 200
                    assert response.json()["success"] == True
    
    @pytest.mark.asyncio
    async def test_log_sports_record_calorie_calculation(self, mock_auth_dependency, mock_database):
        """测试运动记录中的卡路里计算准确性"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        # 定义测试参数
        user_weight = 70.0  # 公斤
        duration_minutes = 30  # 分钟
        mets_value = 8.0  # 跑步的MET值
        
        # 手动计算卡路里消耗：体重(kg) × MET × 时间(小时) × 1.05
        # 30分钟 = 0.5小时
        expected_calories = round(user_weight * mets_value * (duration_minutes / 60) * 1.05, 1)

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            # 模拟运动类型存在
            mock_database["sports"].find_one.return_value = {
                "sport_type": "跑步",
                "METs": mets_value,
                "email": "sport@default.com"
            }

            # 模拟用户体重
            with patch("app.services.sports_service.get_user_weight", return_value=user_weight):
                # 捕获insert_one调用以验证计算的卡路里
                inserted_data = {}
                
                async def capture_insert_one(data):
                    inserted_data.update(data)
                    mock_result = MagicMock()
                    mock_result.inserted_id = ObjectId()
                    return mock_result
                
                mock_database["sports_log"].insert_one = capture_insert_one

                async with AsyncClient(app=app, base_url="http://test") as client:
                    response = await client.post(
                        "/sports/log-sports",
                        json={
                            "sport_type": "跑步",
                            "duration_time": duration_minutes,
                            "created_at": datetime.utcnow().isoformat()
                        }
                    )

                    assert response.status_code == 200
                    assert response.json()["success"] == True
                    # 验证计算的卡路里与预期值一致
                    assert abs(inserted_data.get("calories_burned", 0) - expected_calories) < 0.1

    @pytest.mark.asyncio
    async def test_log_sports_record_invalid_sport_type(self, mock_auth_dependency, mock_database):
        """测试记录不存在的运动类型"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = None

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/log-sports",
                    json={
                        "sport_type": "不存在的运动",
                        "duration_time": 30,
                        "created_at": datetime.utcnow().isoformat()
                    }
                )

                assert response.status_code == 404
                assert "未找到" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_search_sports_records(self, mock_auth_dependency, mock_database):
        """测试搜索运动记录"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_cursor = MagicMock()
            record_id = ObjectId()
            mock_cursor.to_list = AsyncMock(return_value=[
                {
                    "_id": record_id,
                    "sport_type": "跑步",
                    "created_at": datetime.utcnow(),
                    "duration_time": 30,
                    "calories_burned": 300.0
                }
            ])
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/search-sports-records",
                    json={
                        "sport_type": "跑步",
                        "start_date": (date.today() - timedelta(days=7)).isoformat(),
                        "end_date": date.today().isoformat()
                    }
                )

                assert response.status_code == 200
                assert len(response.json()) == 1
                assert response.json()[0]["sport_type"] == "跑步"

    @pytest.mark.asyncio
    async def test_get_all_sports_records(self, mock_auth_dependency, mock_database):
        """测试获取全部运动记录"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=[])
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/get-all-sports-records")

                assert response.status_code == 200
                assert isinstance(response.json(), list)

    @pytest.mark.asyncio
    async def test_delete_sports_record_success(self, mock_auth_dependency, mock_database):
        """测试成功删除运动记录"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_result = MagicMock()
            mock_result.deleted_count = 1
            mock_database["sports_log"].delete_one.return_value = mock_result

            record_id = str(ObjectId())
            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get(f"/sports/delete-sport-record/{record_id}")

                assert response.status_code == 200
                assert response.json()["success"] == True
    
    @pytest.mark.asyncio
    async def test_sports_report_statistics_accuracy(self, mock_auth_dependency, mock_database):
        """测试运动报告统计数据的准确性"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        # 准备测试数据
        test_records = [
            {
                "_id": ObjectId(),
                "sport_type": "跑步",
                "created_at": datetime.utcnow(),
                "duration_time": 30,
                "calories_burned": 300.0
            },
            {
                "_id": ObjectId(),
                "sport_type": "跑步",
                "created_at": datetime.utcnow() - timedelta(days=1),
                "duration_time": 45,
                "calories_burned": 450.0
            },
            {
                "_id": ObjectId(),
                "sport_type": "游泳",
                "created_at": datetime.utcnow() - timedelta(days=2),
                "duration_time": 60,
                "calories_burned": 500.0
            }
        ]
        
        # 手动计算统计数据
        total_activities = len(test_records)
        total_duration = sum(record["duration_time"] for record in test_records)
        total_calories = sum(record["calories_burned"] for record in test_records)
        
        # 确定最喜爱的运动（出现次数最多）
        sport_counts = {}
        for record in test_records:
            sport_counts[record["sport_type"]] = sport_counts.get(record["sport_type"], 0) + 1
        favorite_sport = max(sport_counts, key=sport_counts.get)
        
        # 按运动类型分组的详细统计
        sport_details = {}
        for record in test_records:
            sport_type = record["sport_type"]
            if sport_type not in sport_details:
                sport_details[sport_type] = {
                    "count": 0,
                    "total_duration": 0,
                    "total_calories": 0
                }
            sport_details[sport_type]["count"] += 1
            sport_details[sport_type]["total_duration"] += record["duration_time"]
            sport_details[sport_type]["total_calories"] += record["calories_burned"]

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=test_records)
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/sports-report")

                assert response.status_code == 200
                data = response.json()
                
                # 验证统计数据与手动计算一致
                assert data["total_activities"] == total_activities
                assert data["total_duration"] == total_duration
                assert data["total_calories"] == total_calories
                assert data["favorite_sport"] == favorite_sport
                
                # 验证运动类型详情统计
                assert len(data["sport_details"]) == len(sport_details)
                for sport_type, details in sport_details.items():
                    for report_detail in data["sport_details"]:
                        if report_detail["sport_type"] == sport_type:
                            assert report_detail["count"] == details["count"]
                            assert report_detail["total_duration"] == details["total_duration"]
                            assert report_detail["total_calories"] == details["total_calories"]
                            break
                    else:
                        assert False, f"Sport type {sport_type} not found in report details"

    # ==================== 数据格式验证测试 ====================
    
    @pytest.mark.asyncio
    async def test_invalid_mets_negative(self, mock_auth_dependency, mock_database):
        """测试负数METs值"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        async with AsyncClient(app=app, base_url="http://test") as client:
            response = await client.post(
                "/sports/create-sport",
                json={
                    "sport_type": "测试运动",
                    "describe": "测试",
                    "METs": -2.5  # 负数METs
                }
            )

            assert response.status_code == 422  # 验证请求验证错误

    @pytest.mark.asyncio
    async def test_invalid_duration_negative(self, mock_auth_dependency, mock_database):
        """测试负数持续时间"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = {
                "sport_type": "跑步",
                "METs": 8.0,
                "email": "sport@default.com"
            }

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/log-sports",
                    json={
                        "sport_type": "跑步",
                        "duration_time": -30,  # 负数持续时间
                        "created_at": datetime.utcnow().isoformat()
                    }
                )

                assert response.status_code == 422  # 验证请求验证错误

    @pytest.mark.asyncio
    async def test_invalid_date_format(self, mock_auth_dependency, mock_database):
        """测试无效的日期格式"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = {
                "sport_type": "跑步",
                "METs": 8.0,
                "email": "sport@default.com"
            }

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/log-sports",
                    json={
                        "sport_type": "跑步",
                        "duration_time": 30,
                        "created_at": "2023/13/32"  # 无效的日期格式
                    }
                )

                assert response.status_code == 422  # 验证请求验证错误

    @pytest.mark.asyncio
    async def test_invalid_date_range(self, mock_auth_dependency, mock_database):
        """测试结束日期早于开始日期的情况"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        async with AsyncClient(app=app, base_url="http://test") as client:
            response = await client.post(
                "/sports/search-sports-records",
                json={
                    "start_date": date.today().isoformat(),
                    "end_date": (date.today() - timedelta(days=1)).isoformat()  # 结束日期早于开始日期
                }
            )

            assert response.status_code == 422  # 验证请求验证错误
    
    # ==================== 边界条件测试 ====================
    
    @pytest.mark.asyncio
    async def test_minimum_mets_value(self, mock_auth_dependency, mock_database):
        """测试最小METs值"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = None
            mock_result = MagicMock()
            mock_result.inserted_id = ObjectId()
            mock_database["sports"].insert_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/create-sport",
                    json={
                        "sport_type": "轻度活动",
                        "describe": "轻度运动",
                        "METs": 0.1  # 接近最小值
                    }
                )

                assert response.status_code == 200  # 应该成功

    @pytest.mark.asyncio
    async def test_maximum_mets_value(self, mock_auth_dependency, mock_database):
        """测试最大METs值"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = None
            mock_result = MagicMock()
            mock_result.inserted_id = ObjectId()
            mock_database["sports"].insert_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.post(
                    "/sports/create-sport",
                    json={
                        "sport_type": "高强度运动",
                        "describe": "高强度运动",
                        "METs": 23.0  # 最大METs值
                    }
                )

                assert response.status_code == 200  # 应该成功
    
    @pytest.mark.asyncio
    async def test_minimum_duration_value(self, mock_auth_dependency, mock_database):
        """测试最小持续时间值"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_database["sports"].find_one.return_value = {
                "sport_type": "跑步",
                "METs": 8.0,
                "email": "sport@default.com"
            }
            
            with patch("app.services.sports_service.get_user_weight", return_value=70.0):
                mock_result = MagicMock()
                mock_result.inserted_id = ObjectId()
                mock_database["sports_log"].insert_one.return_value = mock_result

                async with AsyncClient(app=app, base_url="http://test") as client:
                    response = await client.post(
                        "/sports/log-sports",
                        json={
                            "sport_type": "跑步",
                            "duration_time": 1,  # 1分钟
                            "created_at": datetime.utcnow().isoformat()
                        }
                    )

                    assert response.status_code == 200  # 应该成功
    
    # ==================== 异常情况测试 ====================
    
    @pytest.mark.asyncio
    async def test_delete_nonexistent_sport_type(self, mock_auth_dependency, mock_database):
        """测试删除不存在的运动类型"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_result = MagicMock()
            mock_result.deleted_count = 0  # 未删除任何记录
            mock_database["sports"].delete_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/delete-sport/不存在的运动")

                assert response.status_code == 404  # 应该返回未找到错误

    @pytest.mark.asyncio
    async def test_delete_nonexistent_sport_record(self, mock_auth_dependency, mock_database):
        """测试删除不存在的运动记录"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_result = MagicMock()
            mock_result.deleted_count = 0  # 未删除任何记录
            mock_database["sports_log"].delete_one.return_value = mock_result

            record_id = str(ObjectId())
            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get(f"/sports/delete-sport-record/{record_id}")

                assert response.status_code == 404  # 应该返回未找到错误
    
    @pytest.mark.asyncio
    async def test_empty_sports_report(self, mock_auth_dependency, mock_database):
        """测试空的运动记录列表生成报告"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=[])  # 空记录列表
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                response = await client.get("/sports/sports-report")

                assert response.status_code == 200
                data = response.json()
                # 验证空报告的统计数据
                assert data["total_activities"] == 0
                assert data["total_duration"] == 0
                assert data["total_calories"] == 0
                assert data["favorite_sport"] is None or data["favorite_sport"] == ""
                assert len(data["sport_details"]) == 0


# ==================== 集成测试 ====================

class TestSportsIntegration:
    """运动功能端到端集成测试"""

    @pytest.mark.asyncio
    async def test_complete_sports_workflow(self, mock_auth_dependency, mock_database):
        """测试完整的运动记录流程"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            # 1. 创建自定义运动类型
            mock_database["sports"].find_one.return_value = None
            mock_result = MagicMock()
            mock_result.inserted_id = ObjectId()
            mock_database["sports"].insert_one.return_value = mock_result

            async with AsyncClient(app=app, base_url="http://test") as client:
                create_response = await client.post(
                    "/sports/create-sport",
                    json={"sport_type": "篮球", "describe": "团队运动", "METs": 6.5}
                )
                assert create_response.status_code == 200

                # 2. 记录运动
                mock_database["sports"].find_one.return_value = {
                    "sport_type": "篮球",
                    "METs": 6.5,
                    "email": "test@example.com"
                }

                with patch("app.services.sports_service.get_user_weight", return_value=70.0):
                    mock_database["sports_log"].insert_one.return_value = mock_result

                    # 计算预期卡路里
                    expected_calories = round(70.0 * 6.5 * (60 / 60) * 1.05, 1)  # 60分钟 = 1小时
                    
                    # 捕获insert_one调用以验证计算的卡路里
                    inserted_data = {}
                    
                    async def capture_insert_one(data):
                        inserted_data.update(data)
                        return mock_result
                    
                    mock_database["sports_log"].insert_one = capture_insert_one

                    log_response = await client.post(
                        "/sports/log-sports",
                        json={
                            "sport_type": "篮球",
                            "duration_time": 60,
                            "created_at": datetime.utcnow().isoformat()
                        }
                    )
                    
                    assert log_response.status_code == 200
                    # 验证计算的卡路里与预期值一致
                    assert abs(inserted_data.get("calories_burned", 0) - expected_calories) < 0.1

                # 3. 搜索运动记录
                mock_cursor = MagicMock()
                record_id = ObjectId()
                mock_cursor.to_list = AsyncMock(return_value=[
                    {
                        "_id": record_id,
                        "sport_type": "篮球",
                        "created_at": datetime.utcnow(),
                        "duration_time": 60,
                        "calories_burned": expected_calories
                    }
                ])
                mock_database["sports_log"].find.return_value = mock_cursor

                search_response = await client.post(
                    "/sports/search-sports-records",
                    json={"sport_type": "篮球"}
                )
                assert search_response.status_code == 200
                assert len(search_response.json()) > 0


# ==================== 高级路由集成测试 ====================

class TestSportsAdvancedIntegration:
    """高级路由集成测试，涵盖更复杂的业务场景"""

    @pytest.mark.asyncio
    async def test_sports_crud_operations(self, mock_auth_dependency, mock_database):
        """测试运动类型完整CRUD操作"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            async with AsyncClient(app=app, base_url="http://test") as client:
                # 1. 创建运动类型
                mock_database["sports"].find_one.return_value = None
                mock_result = MagicMock()
                mock_result.inserted_id = ObjectId()
                mock_database["sports"].insert_one.return_value = mock_result
                
                create_response = await client.post(
                    "/sports/create-sport",
                    json={"sport_type": "羽毛球", "describe": "球类运动", "METs": 5.0}
                )
                assert create_response.status_code == 200
                assert create_response.json()["success"] == True

                # 2. 获取运动类型列表
                mock_cursor = MagicMock()
                mock_cursor.to_list = AsyncMock(return_value=[
                    {"sport_type": "羽毛球", "describe": "球类运动", "METs": 5.0},
                    {"sport_type": "跑步", "describe": "有氧运动", "METs": 8.0}
                ])
                mock_database["sports"].find.return_value = mock_cursor
                
                list_response = await client.get("/sports/get-available-sports-types")
                assert list_response.status_code == 200
                assert len(list_response.json()) == 2

                # 3. 删除运动类型
                mock_delete_result = MagicMock()
                mock_delete_result.deleted_count = 1
                mock_database["sports"].delete_one.return_value = mock_delete_result
                
                delete_response = await client.get("/sports/delete-sport/羽毛球")
                assert delete_response.status_code == 200
                assert delete_response.json()["success"] == True

    @pytest.mark.asyncio
    async def test_sports_record_with_date_range(self, mock_auth_dependency, mock_database):
        """测试带日期范围的运动记录查询"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            # 模拟带日期范围的数据查询
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=[
                {
                    "_id": ObjectId(),
                    "sport_type": "游泳",
                    "created_at": datetime.utcnow() - timedelta(days=2),
                    "duration_time": 45,
                    "calories_burned": 400.0
                },
                {
                    "_id": ObjectId(),
                    "sport_type": "跑步",
                    "created_at": datetime.utcnow() - timedelta(days=1),
                    "duration_time": 30,
                    "calories_burned": 300.0
                }
            ])
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                # 测试日期范围查询
                start_date = (date.today() - timedelta(days=7)).isoformat()
                end_date = date.today().isoformat()
                
                response = await client.post(
                    "/sports/search-sports-records",
                    json={
                        "start_date": start_date,
                        "end_date": end_date
                    }
                )
                
                assert response.status_code == 200
                assert len(response.json()) == 2
                # 验证返回的数据结构
                for record in response.json():
                    assert all(key in record for key in ["record_id", "sport_type", "created_at", "duration_time", "calories_burned"])

    @pytest.mark.asyncio
    async def test_error_handling_in_sports_flow(self, mock_auth_dependency, mock_database):
        """测试运动记录流程中的错误处理"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            async with AsyncClient(app=app, base_url="http://test") as client:
                # 1. 测试记录不存在的运动类型
                mock_database["sports"].find_one.return_value = None
                
                log_response = await client.post(
                    "/sports/log-sports",
                    json={
                        "sport_type": "不存在的运动",
                        "duration_time": 30
                    }
                )
                assert log_response.status_code == 404
                assert "未找到" in log_response.json()["detail"]

                # 2. 测试创建重复的运动类型
                mock_database["sports"].find_one.return_value = {"sport_type": "重复运动"}
                
                create_response = await client.post(
                    "/sports/create-sport",
                    json={"sport_type": "重复运动", "describe": "测试", "METs": 3.0}
                )
                assert create_response.status_code == 400
                assert "已存在" in create_response.json()["detail"]

                # 3. 测试搜索无效日期范围
                search_response = await client.post(
                    "/sports/search-sports-records",
                    json={
                        "start_date": date.today().isoformat(),
                        "end_date": (date.today() - timedelta(days=1)).isoformat()
                    }
                )
                assert search_response.status_code == 422  # 验证错误码

    @pytest.mark.asyncio
    async def test_sports_report_generation(self, mock_auth_dependency, mock_database):
        """测试运动报告生成的完整流程"""
        from app.routers.auth import get_current_user
        app.dependency_overrides[get_current_user] = mock_auth_dependency

        # 准备测试数据
        test_records = [
            {
                "_id": ObjectId(),
                "sport_type": "跑步",
                "created_at": datetime.utcnow(),
                "duration_time": 30,
                "calories_burned": 300.0
            },
            {
                "_id": ObjectId(),
                "sport_type": "跑步",
                "created_at": datetime.utcnow() - timedelta(days=1),
                "duration_time": 45,
                "calories_burned": 450.0
            },
            {
                "_id": ObjectId(),
                "sport_type": "游泳",
                "created_at": datetime.utcnow() - timedelta(days=2),
                "duration_time": 60,
                "calories_burned": 500.0
            }
        ]
        
        # 手动计算统计数据
        total_activities = len(test_records)
        total_duration = sum(record["duration_time"] for record in test_records)
        total_calories = sum(record["calories_burned"] for record in test_records)

        with patch("app.services.sports_service.get_database", return_value=mock_database):
            # 模拟一周内的运动数据
            mock_cursor = MagicMock()
            mock_cursor.to_list = AsyncMock(return_value=test_records)
            mock_database["sports_log"].find.return_value = mock_cursor

            async with AsyncClient(app=app, base_url="http://test") as client:
                # 获取运动报告
                report_response = await client.get("/sports/sports-report")
                
                assert report_response.status_code == 200
                report = report_response.json()
                
                # 验证报告数据与手动计算一致
                assert report["total_activities"] == total_activities
                assert report["total_duration"] == total_duration
                assert report["total_calories"] == total_calories
                assert report["favorite_sport"] == "跑步"  # 因为跑步次数最多
                assert len(report["sport_details"]) == 2  # 两种运动类型


# 清理依赖覆盖
@pytest.fixture(autouse=True)
def cleanup_overrides():
    """每个测试后清理依赖覆盖"""
    yield
    app.dependency_overrides.clear()


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
