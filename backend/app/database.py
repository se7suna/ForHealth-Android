from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings

# MongoDB 客户端
client = None
database = None


async def connect_to_mongo():
    """连接到 MongoDB"""
    global client, database
    client = AsyncIOMotorClient(settings.MONGODB_URL)
    database = client[settings.DATABASE_NAME]
    print(f"✅ 成功连接到 MongoDB: {settings.DATABASE_NAME}")


async def close_mongo_connection():
    """关闭 MongoDB 连接"""
    global client
    if client:
        client.close()
        print("✅ MongoDB 连接已关闭")


def get_database():
    """获取数据库实例"""
    return database
