"""
数据库初始化脚本
用于从数据集文件初始化食物数据库
支持覆盖和追加模式
"""
import asyncio
import json
import argparse
from datetime import datetime
from pathlib import Path
from motor.motor_asyncio import AsyncIOMotorClient
from typing import List, Dict, Any


class DatabaseInitializer:
    """数据库初始化器"""
    
    def __init__(self, dataset_path: str, db_url: str = "mongodb://localhost:27017", db_name: str = "for_health"):
        """
        初始化
        
        Args:
            dataset_path: 数据集文件路径
            db_url: MongoDB连接URL
            db_name: 数据库名称
        """
        self.dataset_path = Path(dataset_path)
        self.db_url = db_url
        self.db_name = db_name
        self.client = None
        self.db = None
    
    async def connect(self):
        """连接数据库"""
        try:
            self.client = AsyncIOMotorClient(self.db_url, serverSelectionTimeoutMS=5000)
            await self.client.admin.command('ping')
            self.db = self.client[self.db_name]
            print(f"✓ 成功连接到数据库: {self.db_name}")
            return True
        except Exception as e:
            print(f"✗ 数据库连接失败: {e}")
            return False
    
    def load_dataset(self) -> Dict[str, Any]:
        """加载数据集文件"""
        try:
            if not self.dataset_path.exists():
                print(f"✗ 数据集文件不存在: {self.dataset_path}")
                return None
            
            with open(self.dataset_path, 'r', encoding='utf-8') as f:
                dataset = json.load(f)
            
            print(f"✓ 成功加载数据集: {self.dataset_path.name}")
            print(f"  版本: {dataset.get('version', '未知')}")
            print(f"  描述: {dataset.get('description', '无')}")
            print(f"  食物数量: {len(dataset.get('foods', []))}")
            
            return dataset
        except json.JSONDecodeError as e:
            print(f"✗ JSON解析错误: {e}")
            return None
        except Exception as e:
            print(f"✗ 加载数据集失败: {e}")
            return None
    
    async def clear_collection(self, collection_name: str):
        """清空集合"""
        try:
            result = await self.db[collection_name].delete_many({})
            print(f"✓ 已清空集合 '{collection_name}': 删除 {result.deleted_count} 条数据")
            return True
        except Exception as e:
            print(f"✗ 清空集合失败: {e}")
            return False
    
    async def get_existing_foods(self) -> List[str]:
        """获取已存在的食物名称列表"""
        try:
            cursor = self.db.foods.find({}, {"name": 1})
            foods = await cursor.to_list(length=None)
            return [food.get("name") for food in foods]
        except Exception as e:
            print(f"✗ 获取现有食物列表失败: {e}")
            return []
    
    async def insert_foods(self, foods: List[Dict[str, Any]], skip_existing: bool = False):
        """
        插入食物数据
        
        Args:
            foods: 食物数据列表
            skip_existing: 是否跳过已存在的食物
        """
        if not foods:
            print("✗ 没有要插入的食物数据")
            return
        
        # 添加时间戳
        now = datetime.utcnow()
        for food in foods:
            food['created_at'] = now
            food['updated_at'] = now
        
        success_count = 0
        skip_count = 0
        error_count = 0
        
        if skip_existing:
            # 获取已存在的食物名称
            existing_names = set(await self.get_existing_foods())
            print(f"  数据库中已有 {len(existing_names)} 个食物")
        
        for food in foods:
            try:
                food_name = food.get('name', '未命名')
                
                # 如果需要跳过已存在的食物
                if skip_existing and food_name in existing_names:
                    skip_count += 1
                    print(f"  ⊘ 跳过已存在的食物: {food_name}")
                    continue
                
                # 插入食物
                await self.db.foods.insert_one(food)
                success_count += 1
                print(f"  ✓ 已插入: {food_name} ({food.get('category', '未分类')})")
                
            except Exception as e:
                error_count += 1
                print(f"  ✗ 插入失败: {food.get('name', '未知')} - {e}")
        
        print(f"\n插入结果:")
        print(f"  成功: {success_count} 个")
        if skip_count > 0:
            print(f"  跳过: {skip_count} 个")
        if error_count > 0:
            print(f"  失败: {error_count} 个")
    
    async def init_database(self, overwrite: bool = False, skip_existing: bool = False):
        """
        初始化数据库
        
        Args:
            overwrite: 是否覆盖现有数据（清空后重新插入）
            skip_existing: 是否跳过已存在的食物（仅在不覆盖时有效）
        """
        print("=" * 80)
        print("开始初始化数据库")
        print("=" * 80)
        print(f"时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"模式: {'覆盖模式' if overwrite else ('跳过已存在' if skip_existing else '追加模式')}")
        print("=" * 80)
        
        # 1. 连接数据库
        if not await self.connect():
            return False
        
        # 2. 加载数据集
        dataset = self.load_dataset()
        if not dataset:
            return False
        
        foods = dataset.get('foods', [])
        if not foods:
            print("✗ 数据集中没有食物数据")
            return False
        
        # 3. 处理覆盖模式
        if overwrite:
            print("\n⚠ 警告: 覆盖模式 - 将删除所有现有食物数据")
            response = input("确认继续? (yes/no): ").strip().lower()
            if response != 'yes':
                print("✗ 操作已取消")
                return False
            
            if not await self.clear_collection("foods"):
                return False
        
        # 4. 插入食物数据
        print(f"\n开始插入 {len(foods)} 个食物...")
        print("-" * 80)
        
        await self.insert_foods(foods, skip_existing=skip_existing and not overwrite)
        
        # 5. 统计信息
        print("\n" + "=" * 80)
        print("数据库统计:")
        print("=" * 80)
        
        total_count = await self.db.foods.count_documents({})
        print(f"食物总数: {total_count}")
        
        # 按类别统计
        pipeline = [
            {"$group": {"_id": "$category", "count": {"$sum": 1}}},
            {"$sort": {"count": -1}}
        ]
        categories = await self.db.foods.aggregate(pipeline).to_list(length=None)
        
        print("\n按类别统计:")
        for cat in categories:
            cat_name = cat['_id'] if cat['_id'] else '未分类'
            print(f"  {cat_name}: {cat['count']} 个")
        
        print("\n" + "=" * 80)
        print("✓ 数据库初始化完成!")
        print("=" * 80)
        
        return True
    
    async def close(self):
        """关闭数据库连接"""
        if self.client:
            self.client.close()
            print("\n✓ 数据库连接已关闭")


async def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description='初始化食物数据库',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  # 追加模式（保留现有数据，添加新数据）
  python init_database.py
  
  # 覆盖模式（删除所有现有数据，重新初始化）
  python init_database.py --overwrite
  
  # 跳过已存在的食物（仅添加新食物）
  python init_database.py --skip-existing
  
  # 使用自定义数据集文件
  python init_database.py --dataset my_foods.json
        """
    )
    
    parser.add_argument(
        '--dataset',
        type=str,
        default='initial_foods_dataset.json'
    )
    
    parser.add_argument(
        '--overwrite',
        action='store_true',
        default=True,
        help='覆盖模式：删除所有现有数据后重新初始化（默认启用）'
    )
    
    parser.add_argument(
        '--skip-existing',
        action='store_true',
        help='跳过已存在的食物（仅在非覆盖模式下有效）'
    )
    
    parser.add_argument(
        '--db-url',
        type=str,
        default='mongodb://localhost:27017',
        help='MongoDB连接URL (默认: mongodb://localhost:27017)'
    )
    
    parser.add_argument(
        '--db-name',
        type=str,
        default='for_health',
        help='数据库名称 (默认: for_health)'
    )
    
    args = parser.parse_args()
    
    # 创建初始化器
    initializer = DatabaseInitializer(
        dataset_path=args.dataset,
        db_url=args.db_url,
        db_name=args.db_name
    )
    
    try:
        # 执行初始化
        success = await initializer.init_database(
            overwrite=args.overwrite,
            skip_existing=args.skip_existing
        )
        
        if success:
            print("\n🎉 初始化成功！")
        else:
            print("\n❌ 初始化失败！")
            
    except KeyboardInterrupt:
        print("\n\n⚠ 操作被用户中断")
    except Exception as e:
        print(f"\n❌ 发生错误: {e}")
    finally:
        await initializer.close()


if __name__ == '__main__':
    asyncio.run(main())

