"""
清空生产环境数据库脚本
警告: 此脚本将删除所有数据,请谨慎使用!
"""

import asyncio
from motor.motor_asyncio import AsyncIOMotorClient
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv(".env.production")

MONGODB_URL = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
DATABASE_NAME = os.getenv("DATABASE_NAME", "for_health_prod")


async def clear_database():
    """清空数据库所有集合的数据"""

    print(f"⚠️  警告: 即将清空数据库 '{DATABASE_NAME}'")
    print(f"📍 MongoDB URL: {MONGODB_URL}")
    print("")

    # 二次确认
    confirm = input("请输入 'YES' 来确认清空数据库 (任何其他输入将取消操作): ")

    if confirm != "YES":
        print("❌ 操作已取消")
        return

    try:
        # 连接数据库
        client = AsyncIOMotorClient(MONGODB_URL)
        db = client[DATABASE_NAME]

        print(f"\n🔗 已连接到数据库: {DATABASE_NAME}")

        # 获取所有集合
        collections = await db.list_collection_names()

        if not collections:
            print("ℹ️  数据库中没有集合")
            return

        print(f"\n📋 找到 {len(collections)} 个集合:")
        for collection_name in collections:
            count = await db[collection_name].count_documents({})
            print(f"  - {collection_name}: {count} 条记录")

        print(f"\n🗑️  开始清空数据...")

        # 清空所有集合
        deleted_total = 0
        for collection_name in collections:
            result = await db[collection_name].delete_many({})
            deleted_total += result.deleted_count
            print(f"  ✅ {collection_name}: 删除了 {result.deleted_count} 条记录")

        print(f"\n✅ 清空完成! 共删除 {deleted_total} 条记录")

        # 关闭连接
        client.close()

    except Exception as e:
        print(f"\n❌ 错误: {e}")
        raise


async def drop_database():
    """完全删除数据库(包括集合结构)"""

    print(f"⚠️  警告: 即将完全删除数据库 '{DATABASE_NAME}'")
    print(f"📍 MongoDB URL: {MONGODB_URL}")
    print(f"⚠️  这将删除数据库及所有集合结构!")
    print("")

    # 二次确认
    confirm = input("请输入 'DELETE DATABASE' 来确认删除整个数据库 (任何其他输入将取消操作): ")

    if confirm != "DELETE DATABASE":
        print("❌ 操作已取消")
        return

    try:
        # 连接数据库
        client = AsyncIOMotorClient(MONGODB_URL)

        print(f"\n🔗 已连接到 MongoDB")

        # 删除数据库
        await client.drop_database(DATABASE_NAME)

        print(f"✅ 数据库 '{DATABASE_NAME}' 已完全删除")

        # 关闭连接
        client.close()

    except Exception as e:
        print(f"\n❌ 错误: {e}")
        raise


async def show_database_info():
    """显示数据库信息"""

    try:
        # 连接数据库
        client = AsyncIOMotorClient(MONGODB_URL)
        db = client[DATABASE_NAME]

        print(f"📊 数据库信息:")
        print(f"  - 数据库名称: {DATABASE_NAME}")
        print(f"  - MongoDB URL: {MONGODB_URL}")
        print("")

        # 获取所有集合
        collections = await db.list_collection_names()

        if not collections:
            print("ℹ️  数据库中没有集合")
            return

        print(f"📋 集合列表 (共 {len(collections)} 个):")
        total_documents = 0
        for collection_name in collections:
            count = await db[collection_name].count_documents({})
            total_documents += count
            print(f"  - {collection_name}: {count} 条记录")

        print(f"\n📈 总计: {total_documents} 条记录")

        # 关闭连接
        client.close()

    except Exception as e:
        print(f"\n❌ 错误: {e}")
        raise


def main():
    """主函数"""
    print("=" * 60)
    print("For Health - 数据库管理工具")
    print("=" * 60)
    print("")
    print("请选择操作:")
    print("  1. 查看数据库信息")
    print("  2. 清空所有集合数据 (保留集合结构)")
    print("  3. 完全删除数据库 (包括集合结构)")
    print("  0. 退出")
    print("")

    choice = input("请输入选项 (0-3): ")

    if choice == "1":
        asyncio.run(show_database_info())
    elif choice == "2":
        asyncio.run(clear_database())
    elif choice == "3":
        asyncio.run(drop_database())
    elif choice == "0":
        print("👋 再见!")
    else:
        print("❌ 无效的选项")


if __name__ == "__main__":
    main()
