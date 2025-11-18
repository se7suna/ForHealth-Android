from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings

# MongoDB 客户端
client = None
database = None


async def connect_to_mongo():
    """连接到 MongoDB"""
    global client, database
    try:
        client = AsyncIOMotorClient(
            settings.MONGODB_URL,
            serverSelectionTimeoutMS=5000  # 5秒超时
        )
        # 测试连接
        await client.admin.command('ping')
        database = client[settings.DATABASE_NAME]
        print(f"✅ 成功连接到 MongoDB: {settings.DATABASE_NAME}")
    except Exception as e:
        print(f"⚠️  MongoDB 连接失败: {e}")
        print("⚠️  服务器将继续运行，但数据库功能将不可用")
        client = None
        database = None


async def close_mongo_connection():
    """关闭 MongoDB 连接"""
    global client
    if client:
        client.close()
        print("✅ MongoDB 连接已关闭")


def get_database():
    """获取数据库实例"""
    if database is None:
        raise Exception("数据库未连接，请先启动 MongoDB 服务")
    return database

# 初始化运动表，填入默认运动类型和卡路里消耗
from app.config import settings
async def initialize_sports_table():
    db = get_database()
    for sport in settings.DefaultSports:
        existing = await db["sports"].find_one({"sport_type": sport["sport_type"],"email": settings.DEFAULT_SPORT_EMAIL})
        if not existing:
            print(sport["sport_type"])
            await db["sports"].insert_one(sport)


# 初始化管理员用户
from app.utils.security import get_password_hash
async def initialize_default_user():
    db = get_database()
    existing = await db["users"].find_one({"email": settings.DEFAULT_AUTH_EMAIL})
    if not existing:
        await db["users"].insert_one({
            "email": settings.DEFAULT_AUTH_EMAIL,
            "password": get_password_hash(settings.DEFAULT_PASSWORD),
            "name": "Default User"
        })

