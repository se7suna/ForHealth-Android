# 将 backend 目录添加到 Python 路径
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.parent.absolute())
sys.path.insert(0, backend_path)

import pytest
import pytest_asyncio
from datetime import datetime, date, timedelta
from httpx import AsyncClient
from app.main import app
from app.database import get_database

# ================== 测试数据准备 ==================
from app.config import settings
TEST_USER = {
    "email": settings.USER_EMAIL,
    "password": settings.USER_PASSWORD
}

# 用于存储测试过程中创建的资源 ID
test_record_id = None


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


@pytest_asyncio.fixture(autouse=True)
async def cleanup_weight_records():
    """测试后清理体重记录"""
    # 不在 setup 阶段清理，避免数据库未连接的问题
    yield

    # 测试后清理
    try:
        db = get_database()
        await db.weight_records.delete_many({"user_email": TEST_USER["email"]})
    except Exception:
        # 如果数据库未连接，忽略清理错误
        pass


# ================== 测试：创建体重记录 ==================
@pytest.mark.asyncio
async def test_create_weight_record(auth_client):
    """测试创建体重记录"""
    global test_record_id

    response = await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 70.5,
            "recorded_at": datetime.utcnow().isoformat(),
            "notes": "晨起空腹"
        }
    )

    assert response.status_code == 201, f"创建失败: {response.text}"
    data = response.json()
    assert data["weight"] == 70.5
    assert data["notes"] == "晨起空腹"
    assert "id" in data
    assert "created_at" in data

    test_record_id = data["id"]


# ================== 测试：创建体重记录 - 无效数据 ==================
@pytest.mark.asyncio
async def test_create_weight_record_invalid_data(auth_client):
    """测试创建体重记录 - 无效数据"""
    # 测试负数体重
    response = await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": -10,
            "recorded_at": datetime.utcnow().isoformat(),
        }
    )
    assert response.status_code == 422

    # 测试超大体重
    response = await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 600,
            "recorded_at": datetime.utcnow().isoformat(),
        }
    )
    assert response.status_code == 422


# ================== 测试：获取体重记录列表 ==================
@pytest.mark.asyncio
async def test_get_weight_records(auth_client):
    """测试获取体重记录列表"""
    # 先创建几条记录
    for i in range(3):
        await auth_client.post(
            "/api/user/weight-record",
            json={
                "weight": 70.0 + i,
                "recorded_at": (datetime.utcnow() - timedelta(days=i)).isoformat(),
                "notes": f"记录{i}"
            }
        )

    # 获取记录列表
    response = await auth_client.get("/api/user/weight-records")

    assert response.status_code == 200, f"获取失败: {response.text}"
    data = response.json()
    assert data["total"] == 3
    assert len(data["records"]) == 3
    # 验证按时间倒序排列
    assert data["records"][0]["notes"] == "记录0"


# ================== 测试：按日期范围查询体重记录 ==================
@pytest.mark.asyncio
async def test_get_weight_records_with_date_range(auth_client):
    """测试按日期范围查询体重记录"""
    today = datetime.utcnow().date()

    # 创建不同日期的记录
    await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 70.0,
            "recorded_at": (datetime.utcnow() - timedelta(days=5)).isoformat(),
        }
    )
    await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 71.0,
            "recorded_at": datetime.utcnow().isoformat(),
        }
    )

    # 查询最近3天的记录
    start_date = (today - timedelta(days=3)).isoformat()
    response = await auth_client.get(
        f"/api/user/weight-records?start_date={start_date}"
    )

    assert response.status_code == 200, f"查询失败: {response.text}"
    data = response.json()
    assert data["total"] == 1
    assert data["records"][0]["weight"] == 71.0


# ================== 测试：更新体重记录 ==================
@pytest.mark.asyncio
async def test_update_weight_record(auth_client):
    """测试更新体重记录"""
    # 先创建一条记录
    create_response = await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 70.0,
            "recorded_at": datetime.utcnow().isoformat(),
            "notes": "原始备注"
        }
    )
    record_id = create_response.json()["id"]

    # 更新记录
    response = await auth_client.put(
        f"/api/user/weight-record/{record_id}",
        json={
            "weight": 71.5,
            "notes": "更新后的备注"
        }
    )

    assert response.status_code == 200, f"更新失败: {response.text}"
    data = response.json()
    assert data["weight"] == 71.5
    assert data["notes"] == "更新后的备注"


# ================== 测试：删除体重记录 ==================
@pytest.mark.asyncio
async def test_delete_weight_record(auth_client):
    """测试删除体重记录"""
    # 先创建一条记录
    create_response = await auth_client.post(
        "/api/user/weight-record",
        json={
            "weight": 70.0,
            "recorded_at": datetime.utcnow().isoformat(),
        }
    )
    record_id = create_response.json()["id"]

    # 删除记录
    response = await auth_client.delete(
        f"/api/user/weight-record/{record_id}"
    )

    assert response.status_code == 200, f"删除失败: {response.text}"
    assert response.json()["message"] == "体重记录删除成功"

    # 验证记录已删除
    get_response = await auth_client.get("/api/user/weight-records")
    assert get_response.json()["total"] == 0


# ================== 测试：未认证访问 ==================
@pytest.mark.asyncio
async def test_unauthorized_access():
    """测试未认证访问"""
    async with AsyncClient(base_url="http://127.0.0.1:8000", timeout=30.0, http2=False) as client:
        # 测试创建记录
        response = await client.post(
            "/api/user/weight-record",
            json={
                "weight": 70.0,
                "recorded_at": datetime.utcnow().isoformat(),
            }
        )
        assert response.status_code == 401

        # 测试获取记录列表
        response = await client.get("/api/user/weight-records")
        assert response.status_code == 401
