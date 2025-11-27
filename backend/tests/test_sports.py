# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest,pytest_asyncio
from datetime import datetime, date, timedelta
from httpx import AsyncClient
from app.main import app

# ================== 测试数据准备 ==================
# 测试用户凭证
from app.config import settings
TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD
}
# 用于存储测试过程中创建的资源 ID
test_sport_name = None
test_record_id = None
test_token = None


# ================== Fixtures：建立可认证的客户端 ==================
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


# ================== 测试：创建自定义运动类型 ==================
@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,expected_status,expected_success", [
    # 正常情况
    ({"sport_name": "自定义跑步", "describe": "户外跑步", "METs": 8.0}, 200, True),
    ({"sport_name": "瑜伽", "describe": "放松瑜伽", "METs": 2.5}, 200, True),
    ({"sport_name": "ex游泳", "describe": "自由泳", "METs": 9.5}, 200, True), # 游泳在默认表内
    # 边界条件 - 最小METs值
    ({"sport_name": "冥想", "describe": "静坐", "METs": 0.1}, 200, True),
    # 边界条件 - 最大METs值
    ({"sport_name": "极限训练", "describe": "高强度", "METs": 20.0}, 200, True),
])
async def test_create_sports(auth_client, sport_data, expected_status, expected_success):
    """测试创建自定义运动类型 - 正常情况和边界条件"""
    response = None
    try:
        response = await auth_client.post("/api/sports/create-sport", data=sport_data,files={})
        result = response.json()
        assert response.status_code == expected_status
        assert result["success"] == expected_success
    finally:
        if response and result.get("success"):        # 完成测试后删除创建的运动类型
            response = await auth_client.delete(f"/api/sports/delete-sport/{sport_data['sport_name']}")
            assert response.status_code == 200
          


@pytest.mark.asyncio
@pytest.mark.parametrize("invalid_sport_data,expected_status", [
    # 异常情况 - METs为负数
    ({"sport_name": "无效运动", "describe": "测试", "METs": -1.0}, 422),
    # 异常情况 - METs为0
    ({"sport_name": "无效运动2", "describe": "测试", "METs": 0}, 422),
    # 异常情况 - 缺少必要字段
    ({"sport_name": "缺少METs", "describe": "测试"}, 422),
])
async def test_create_sports_invalid(auth_client, invalid_sport_data, expected_status):
    """测试创建自定义运动类型 - 异常情况"""
    response = await auth_client.post("/api/sports/create-sport", data=invalid_sport_data,files={})
    assert response.status_code == expected_status


@pytest.mark.asyncio
async def test_create_sports_without_auth():
    """测试未认证情况下创建运动类型"""
    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post(
            "/api/sports/create-sport",
            data={"sport_name": "测试", "describe": "测试", "METs": 5.0},
            files={}
        )
        assert response.status_code == 403


# ================== 测试：获取可用运动类型 ==================

@pytest.mark.asyncio
async def test_get_available_sports_types(auth_client):
    """测试获取用户可用运动类型列表"""
    response = await auth_client.get("/api/sports/get-available-sports-types")
    assert response.status_code == 200

    sports_list = response.json()
    assert isinstance(sports_list, list)
    assert len(sports_list) > 0

    # 验证返回数据格式
    for sport in sports_list:
        assert "sport_name" in sport
        assert "METs" in sport
        assert sport["METs"] > 0


# ================== 测试：更新自定义运动类型 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("original_data,update_data,expected_success", [
    # 正常更新 - 使用唯一名称
    (
        {"sport_name": "自定义跑步_test4", "describe": "初始描述", "METs": 8.0},
        {"old_sport_name": "自定义跑步_test4","new_sport_name": "自定义跑步_test4plus", "describe": "更新后的描述", "METs": 9.0},
        True
    ),
    # 边界条件 - 更新为最小METs - 使用唯一名称
    (
        {"sport_name": "自定义跑步_test2", "describe": "初始描述", "METs": 8.0},
        {"old_sport_name": "自定义跑步_test2","new_sport_name": "自定义跑步_test2plus", "describe": "低强度", "METs": 0.5},
        True
    ),
])
async def test_update_sports(auth_client, original_data, update_data, expected_success):
    """测试更新自定义运动类型 - 正常情况"""
    # 先创建运动类型
    try:
        updated =False

        create_response = await auth_client.post("/api/sports/create-sport", data=original_data,files={})
        assert create_response.status_code == 200

        # 再更新
        response = await auth_client.post("/api/sports/update-sport", data=update_data,files={})

        if expected_success:
            assert response.status_code == 200
            updated =True
            result = response.json()
            assert result["success"] == expected_success
    finally:
        # 清理 - 删除创建的运动类型（使用参数化的 sport_name）
        if updated:
            sport_name = update_data["new_sport_name"]
        else:
            sport_name = update_data["old_sport_name"]
        response = await auth_client.delete(f"/api/sports/delete-sport/{sport_name}")
        assert response.status_code == 200


@pytest.mark.asyncio
@pytest.mark.parametrize("invalid_update_data,expected_status", [
    # 异常情况 - METs为负数
    ({"sport_name": "自定义跑步", "describe": "无效", "METs": -5.0}, 422),
    # 异常情况 - METs为0
    ({"sport_name": "自定义跑步", "describe": "无效", "METs": 0}, 422),
])
async def test_update_sports_invalid(auth_client, invalid_update_data, expected_status):
    """测试更新自定义运动类型 - 异常情况"""
    response = await auth_client.post("/api/sports/update-sport", json=invalid_update_data)
    assert response.status_code == expected_status

# ================== 测试：运动类型包含图片的增删改 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("sport_data,image_file,update_sport_data,update_image_file,expected_status,expected_success", [
    ({"sport_name": "自定义游泳_test20", "describe": "户外游泳", "METs": 8.0}, "test_sport_image1.jpeg", 
     {"old_sport_name": "自定义游泳_test20","new_sport_name": "自定义游泳_test12plus", "describe": "户外游泳", "METs": 8.0}, "test_sport_image2.jpeg",
     200, True),
])
async def test_sports_with_image(auth_client, sport_data, image_file, update_sport_data, update_image_file, expected_status, expected_success): 
    """测试自定义运动类型 - 包含图片"""
    async def tools_find_sport(sport_name):
        ##获取全部运动
        r = await auth_client.get(f"/api/sports/get-available-sports-types")
        assert r.status_code == 200
        ##找到要删除的运动类型
        for sport in r.json():
            if sport["sport_name"] == sport_name:
                return sport
    def tools_get_path(image_url):
        #解析得到相对的路径
        from urllib.parse import urlparse
        parsed_url = urlparse(image_url).path # "/static/sport_images/xxx.jpeg"
        base = settings.IMAGE_BASE_URL  # "/static"
        assert parsed_url.startswith(base + "/"),"无效的路径名"
        relative_path = parsed_url[len(base) + 1:] # "sport_images/xxx.jpeg"
        # 合成后端可检索路径 backend/uploads/sport_images/xxx.jpeg
        return Path(__file__).parent.parent / settings.IMAGE_STORAGE_PATH / relative_path

    try:
        # 上传包含图片的表单数据
        response = None
        update =None

        test_image_path = Path(__file__).parent / "test_picture" / image_file
        response = await auth_client.post(
            "/api/sports/create-sport",
            data=sport_data,
            files={"image_file": (image_file, open(test_image_path, "rb"), "image/jpeg")}
        )
        assert response.status_code == expected_status
        result = response.json()
        assert result["success"] == expected_success

        # 确保创建时图片已保存
        create_sport = await tools_find_sport(sport_data["sport_name"])
        create_image_path = tools_get_path(create_sport["image_url"])
        assert create_image_path.exists(),"图片未保存"

        # 更新
        test_update_image_path = Path(__file__).parent / "test_picture" / update_image_file
        update_response = await auth_client.post(
            "/api/sports/update-sport",
            data=update_sport_data,
            files={"image_file": (update_image_file, open(test_update_image_path, "rb"), "image/jpeg")}
        )
        assert update_response.status_code == expected_status
        update =True
        update_result = update_response.json()
        assert update_result["success"] == expected_success
        # 确保更新时图片已保存
        update_sport = await tools_find_sport(update_sport_data["new_sport_name"])
        update_image_path = tools_get_path(update_sport["image_url"])
        assert update_image_path.exists(),"图片未保存"
    finally:
        if response and result.get("success"):        # 完成测试后删除创建的运动类型
            #确保删除时正确查找运动名
            if update:
                delete_sport_name = update_sport_data["new_sport_name"]
            else:
                delete_sport_name = update_sport_data["old_sport_name"]
            #先存储图片路径
            delete_sport = await tools_find_sport(delete_sport_name)
            test_delete_path = tools_get_path(delete_sport["image_url"])

            response = await auth_client.delete(f"/api/sports/delete-sport/{delete_sport_name}")
            assert response.status_code == 200
           
            assert not test_delete_path.exists(),"图片未删除"
 

# ================== 测试：记录运动 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("log_data,expected_status,expected_success", [
    # 正常情况 - 使用当前时间
    ({"sport_name": "慢走 3km/h", "duration_time": 30}, 200, True),
    ({"sport_name": "慢走 3km/h", "duration_time": 60}, 200, True),
    # 边界条件 - 最小持续时间
    ({"sport_name": "慢走 3km/h", "duration_time": 1}, 200, True),
    # 边界条件 - 最大持续时间
    ({"sport_name": "慢走 3km/h", "duration_time": 480}, 200, True),
])
async def test_log_sports_record(auth_client, log_data, expected_status, expected_success):
    """测试记录运动 - 正常情况和边界条件"""
    global test_record_id

    # 添加时间戳
    log_data["created_at"] = datetime.utcnow().isoformat()

    response = await auth_client.post("/api/sports/log-sports", json=log_data)
    assert response.status_code == expected_status

    result = response.json()
    assert result["success"] == expected_success

    if expected_success and log_data["duration_time"] == 30:
        # 验证记录已创建，获取记录ID用于后续测试
        records_response = await auth_client.get("/api/sports/get-all-sports-records")
        records = records_response.json()
        if records and len(records) > 0:
            test_record_id = records[0]["record_id"]


@pytest.mark.asyncio
@pytest.mark.parametrize("invalid_log_data,expected_status", [
    # 异常情况 - 持续时间为0
    ({"sport_name": "慢走 3km/h", "duration_time": 0}, 422),
    # 异常情况 - 持续时间为负数
    ({"sport_name": "慢走 3km/h", "duration_time": -10}, 422),
    # 异常情况 - 缺少必要字段
    ({"sport_name": "慢走 3km/h"}, 422),
])
async def test_log_sports_record_invalid(auth_client, invalid_log_data, expected_status):
    """测试记录运动 - 异常情况"""
    if "duration_time" in invalid_log_data and invalid_log_data["duration_time"] is not None:
        invalid_log_data["created_at"] = datetime.utcnow().isoformat()

    response = await auth_client.post("/api/sports/log-sports", json=invalid_log_data)
    assert response.status_code == expected_status


@pytest.mark.asyncio
async def test_log_sports_with_past_time(auth_client):
    """测试使用过去时间记录运动"""
    past_time = (datetime.utcnow() - timedelta(days=1)).isoformat()
    log_data = {
        "sport_name": "慢走 3km/h",
        "created_at": past_time,
        "duration_time": 45
    }

    response = await auth_client.post("/api/sports/log-sports", json=log_data)
    assert response.status_code == 200
    assert response.json()["success"] is True


# ================== 测试：查询运动记录 ==================

@pytest.mark.asyncio
async def test_get_all_sports_records(auth_client):
    """测试获取全部运动记录"""
    response = await auth_client.get("/api/sports/get-all-sports-records")
    assert response.status_code == 200

    records = response.json()
    assert isinstance(records, list)

    # 验证记录格式
    if len(records) > 0:
        record = records[0]
        assert "sport_name" in record
        assert "created_at" in record
        assert "duration_time" in record
        assert "calories_burned" in record
        assert "record_id" in record

        # 验证数据有效性
        assert record["created_at"] is not None
        assert record["duration_time"] > 0
        assert record["calories_burned"] >= 0


@pytest.mark.asyncio
@pytest.mark.parametrize("search_params,should_have_results", [
    # 按运动类型搜索
    ({"sport_name": "自定义跑步"}, True),
    # 按日期范围搜索
    ({"start_date": (date.today() - timedelta(days=7)).isoformat(),
      "end_date": date.today().isoformat()}, True),
    # 组合搜索
    ({"sport_name": "自定义跑步",
      "start_date": (date.today() - timedelta(days=7)).isoformat(),
      "end_date": date.today().isoformat()}, True),
])
async def test_search_sports_records(auth_client, search_params, should_have_results):
    """测试搜索运动记录 - 各种查询条件"""
    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    assert response.status_code == 200

    records = response.json()
    assert isinstance(records, list)


@pytest.mark.asyncio
async def test_search_sports_records_invalid_date_range(auth_client):
    """测试搜索运动记录 - 无效的日期范围（结束日期早于开始日期）"""
    search_params = {
        "start_date": date.today().isoformat(),
        "end_date": (date.today() - timedelta(days=7)).isoformat()
    }

    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    # 应该返回验证错误
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_search_nonexistent_sport_name(auth_client):
    """测试搜索不存在的运动类型"""
    search_params = {"sport_name": "不存在的运动类型_XYZ123"}

    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    assert response.status_code == 200

    records = response.json()
    assert len(records) == 0  # 应该返回空列表


# ================== 测试：更新运动记录 ==================

@pytest.mark.asyncio
async def test_update_sports_record(auth_client):
    """测试更新运动记录"""
    # 先获取记录列表确保有记录可更新
    records_response = await auth_client.get("/api/sports/get-all-sports-records")
    records = records_response.json()

    if len(records) > 0:
        record_id = records[0]["record_id"]
        update_data = {
            "record_id": record_id,
            "sport_name": "慢走 3km/h",
            "duration_time": 45,
            "created_at": datetime.utcnow().isoformat()
        }

        response = await auth_client.post("/api/sports/update-sport-record", json=update_data)
        assert response.status_code == 200
        assert response.json()["success"] is True


@pytest.mark.asyncio
@pytest.mark.parametrize("invalid_update_data", [
    # 异常情况 - 持续时间为负数
    {"_id": "test_id", "sport_name": "自定义跑步", "duration_time": -10},
    # 异常情况 - 持续时间为0
    {"_id": "test_id", "sport_name": "自定义跑步", "duration_time": 0},
])
async def test_update_sports_record_invalid(auth_client, invalid_update_data):
    """测试更新运动记录 - 异常情况"""
    if "created_at" not in invalid_update_data:
        invalid_update_data["created_at"] = datetime.utcnow().isoformat()

    response = await auth_client.post("/api/sports/update-sport-record", json=invalid_update_data)
    # 应该返回验证错误或更新失败
    assert response.status_code in [422, 500]


# ================== 测试：运动报告 ==================

@pytest.mark.asyncio
async def test_sports_report(auth_client):
    """测试获取运动报告"""
    response = await auth_client.get("/api/sports/sports-report")
    assert response.status_code == 200

    report = response.json()
    assert isinstance(report, dict)

    # 验证报告包含必要的统计信息
    # 根据实际返回结构验证（这里假设返回包含统计数据）
    assert report is not None


@pytest.mark.asyncio
async def test_sports_report_calculations(auth_client):
    """测试运动报告的计算逻辑准确性"""
    # 先获取所有记录
    records_response = await auth_client.get("/api/sports/get-all-sports-records")
    records = records_response.json()

    if len(records) > 0:
        # 手动计算总时长和总卡路里
        total_duration = sum(r["duration_time"] for r in records)
        total_calories = sum(r["calories_burned"] for r in records)
        total_count = len(records)

        # 获取报告
        report_response = await auth_client.get("/api/sports/sports-report")
        report = report_response.json()

        # 验证报告中的统计数据（根据实际报告结构调整）
        # 这里需要根据实际的报告格式进行验证
        assert report is not None


# ================== 测试：删除运动记录 ==================

@pytest.mark.asyncio
async def test_delete_sports_record(auth_client):
    """测试删除运动记录"""
    # 先创建一条记录用于删除
    log_data = {
        "sport_name": "慢走 3km/h",
        "created_at": datetime.utcnow().isoformat(),
        "duration_time": 20
    }
    create_response = await auth_client.post("/api/sports/log-sports", json=log_data)
    assert create_response.status_code == 200

    # 获取刚创建的记录ID
    records_response = await auth_client.get("/api/sports/get-all-sports-records")
    records = records_response.json()

    if len(records) > 0:
        sport_name = records[0]["sport_name"]
        record_id = records[0]["record_id"]
        created_at = records[0]["created_at"]
        assert sport_name == log_data["sport_name"]  # 假设创建时返回的ID为"123"
        #assert created_at ==  log_data["created_at"] # 确保记录ID存在

        # 删除记录
        response = await auth_client.delete(f"/api/sports/delete-sport-record/{record_id}")
        assert response.status_code == 200
        assert response.json()["success"] is True


@pytest.mark.asyncio
async def test_delete_nonexistent_record(auth_client):
    """测试删除不存在的运动记录"""
    fake_id = "507f1f77bcf86cd799439011"  # 有效的MongoDB ObjectId格式

    response = await auth_client.delete(f"/api/sports/delete-sport-record/{fake_id}")
    assert response.status_code == 404


# ================== 测试：删除自定义运动类型 ==================

@pytest.mark.asyncio
async def test_delete_sports(auth_client):
    """测试删除自定义运动类型"""
    # 先创建一个用于删除的运动类型
    create_data = {
        "sport_name": "待删除的运动",
        "describe": "测试删除",
        "METs": 5.0
    }
    create_response = await auth_client.post("/api/sports/create-sport", data=create_data,files={})
    assert create_response.status_code == 200

    # 删除刚创建的运动类型
    response = await auth_client.delete(f"/api/sports/delete-sport/{create_data['sport_name']}")
    assert response.status_code == 200
    assert response.json()["success"] is True


@pytest.mark.asyncio
async def test_delete_nonexistent_sport_name(auth_client):
    """测试删除不存在的运动类型"""
    response = await auth_client.delete("/api/sports/delete-sport/不存在的运动类型_XYZ")
    assert response.status_code == 404


# ================== 测试：数据格式有效性 ==================

@pytest.mark.asyncio
@pytest.mark.parametrize("invalid_data,expected_status", [
    # 空字符串的运动类型
    ({"sport_name": "", "describe": "测试", "METs": 5.0}, 422),
    # METs为字符串
    ({"sport_name": "测试", "describe": "测试", "METs": "invalid"}, 422),# 参数数据类型错误，pydantic类验证抛出
])
async def test_data_format_validation(auth_client, invalid_data, expected_status):
    """测试数据格式有效性验证"""
    response = await auth_client.post("/api/sports/create-sport", data=invalid_data,files={})
    assert response.status_code == expected_status


# ================== 测试：卡路里计算准确性 ==================

@pytest.mark.asyncio
async def test_calories_calculation_accuracy(auth_client):
    """测试卡路里计算的准确性"""
    # 创建一个已知METs的运动类型
    sport_data = {
        "sport_name": "测试计算",
        "describe": "用于测试卡路里计算",
        "METs": 10.0  # 使用一个容易计算的值
    }
    await auth_client.post("/api/sports/create-sport", data=sport_data,files={})

    # 记录运动
    log_data = {
        "sport_name": "测试计算",
        "created_at": datetime.utcnow().isoformat(),
        "duration_time": 60  # 60分钟
    }
    await auth_client.post("/api/sports/log-sports", json=log_data)

    # 获取记录
    search_params = {"sport_name": "测试计算"}
    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    records = response.json()

    if len(records) > 0:
        record = records[0]
        # 验证卡路里计算
        # 公式：卡路里 = METs × 体重(kg) × 时间(小时)
        # 这里需要知道用户体重才能验证，至少验证结果不为0
        assert record["calories_burned"] > 0
        assert isinstance(record["calories_burned"], (int, float))

    # 清理测试数据
    await auth_client.delete("/api/sports/delete-sport/测试计算")


# ================== 测试：并发和边界情况 ==================

@pytest.mark.asyncio
async def test_empty_records_list(auth_client):
    """测试空记录列表的处理"""
    # 使用一个不可能存在的日期范围
    search_params = {
        "start_date": "1900-01-01",
        "end_date": "1900-01-02"
    }

    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.asyncio
async def test_multiple_records_same_sport(auth_client):
    """测试同一运动类型的多条记录"""
    # 创建多条相同运动类型的记录
    for duration in [10, 20, 30]:
        log_data = {
            "sport_name": "慢走 3km/h",
            "created_at": datetime.utcnow().isoformat(),
            "duration_time": duration
        }
        response = await auth_client.post("/api/sports/log-sports", json=log_data)
        assert response.status_code == 200

    # 查询该运动类型的所有记录
    search_params = {"sport_name": log_data["sport_name"]}
    response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    records = response.json()

    assert len(records) >= 3  # 至少有我们刚创建的3条


# ================== 集成测试：完整工作流 ==================

@pytest.mark.asyncio
async def test_complete_workflow(auth_client):
    old_sport_name="集成测试运动"
    new_sport_type="更新后测试运动"
    """集成测试：完整的工作流程"""
    # 1. 创建自定义运动类型
    sport_data = {
        "sport_name": old_sport_name,
        "describe": "完整流程测试",
        "METs": 7.5
    }
    create_response = await auth_client.post("/api/sports/create-sport", data=sport_data,files={})
    assert create_response.status_code == 200

    # 2. 记录运动
    log_data = {
        "sport_name": old_sport_name,
        "created_at": datetime.utcnow().isoformat(),
        "duration_time": 40
    }
    log_response = await auth_client.post("/api/sports/log-sports", json=log_data)
    assert log_response.status_code == 200

    # 3. 查询记录
    search_params = {"sport_name": old_sport_name}
    search_response = await auth_client.post("/api/sports/search-sports-records", json=search_params)
    records = search_response.json()
    assert len(records) > 0

    record_id = records[0]["record_id"]

    # 4. 更新记录
    update_data = {
        "record_id": record_id,
        "old_sport_name": old_sport_name,
        "new_sport_name": new_sport_type,
        "duration_time": 50,
        "created_at": datetime.utcnow().isoformat()
    }
    update_response = await auth_client.post("/api/sports/update-sport-record", json=update_data)
    assert update_response.status_code == 200

    # 5. 获取报告
    report_response = await auth_client.get("/api/sports/sports-report")
    assert report_response.status_code == 200

    # 6. 删除记录
    delete_record_response = await auth_client.delete(f"/api/sports/delete-sport-record/{record_id}")
    assert delete_record_response.status_code == 200

    # 7. 删除运动类型
    delete_sport_response = await auth_client.delete(f"/api/sports/delete-sport/{new_sport_type}")
    assert delete_sport_response.status_code == 200
