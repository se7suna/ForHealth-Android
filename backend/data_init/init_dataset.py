"""
数据库初始化脚本
用于从数据集文件初始化食物数据库
支持覆盖和追加模式
注意：数据集文件中的每个食物必须包含 image_url 字段
图片将从外部URL下载并保存到本地文件系统
"""
import asyncio
import json
import argparse
from datetime import datetime
from pathlib import Path
from motor.motor_asyncio import AsyncIOMotorClient
from typing import List, Dict, Any, Optional
import httpx
import uuid
import io
from PIL import Image
import sys

# 添加项目根目录到路径，以便导入应用模块
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    from app.config import settings
    from app.utils.image_storage import get_image_url
    CONFIG_AVAILABLE = True
except ImportError:
    CONFIG_AVAILABLE = False
    print("⚠ 警告: 无法导入配置模块，将使用默认路径")


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
    
    async def clear_collection(self, collection_name: str, delete_images: bool = False):
        """
        清空集合
        
        Args:
            collection_name: 集合名称
            delete_images: 是否同时删除关联的图片文件
        """
        try:
            deleted_image_count = 0
            
            # 如果需要删除图片，先获取所有图片URL
            if delete_images and collection_name == "foods":
                try:
                    cursor = self.db[collection_name].find({}, {"image_url": 1})
                    foods = await cursor.to_list(length=None)
                    
                    # 获取项目根目录（backend/）
                    backend_dir = Path(__file__).parent.parent
                    
                    for food in foods:
                        image_url = food.get("image_url")
                        if image_url:
                            # 只删除本地图片文件（以 http://localhost 或相对路径开头的）
                            if image_url.startswith("http://localhost") or image_url.startswith("https://localhost") or not image_url.startswith(("http://", "https://")):
                                # 提取文件路径
                                if image_url.startswith(("http://localhost", "https://localhost")):
                                    # 从完整URL中提取相对路径
                                    from urllib.parse import urlparse
                                    parsed = urlparse(image_url)
                                    relative_path = parsed.path.lstrip("/")
                                    # 去掉 static/ 前缀
                                    if relative_path.startswith("static/"):
                                        relative_path = relative_path.replace("static/", "", 1)
                                else:
                                    # 假设已经是相对路径
                                    relative_path = image_url
                                
                                # 构建完整文件路径
                                if relative_path.startswith("food_images/"):
                                    file_path = backend_dir / "uploads" / relative_path
                                else:
                                    file_path = backend_dir / "uploads" / "food_images" / relative_path
                                
                                # 删除文件
                                if file_path.exists() and file_path.is_file():
                                    file_path.unlink()
                                    deleted_image_count += 1
                    
                    if deleted_image_count > 0:
                        print(f"  ✓ 已删除 {deleted_image_count} 个图片文件")
                except Exception as e:
                    print(f"  ⚠ 删除图片文件时出错: {e}")
            
            # 清空集合
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
    
    async def download_and_save_image(self, image_url: str, food_name: str) -> Optional[str]:
        """
        从外部URL下载图片并保存到本地文件系统（与image_storage.py的存储方式一致）
        
        Args:
            image_url: 外部图片URL
            food_name: 食物名称（用于生成文件名）
            
        Returns:
            本地图片访问URL，如果下载失败则返回None
        """
        if not image_url or not image_url.startswith(("http://", "https://")):
            return None
        
        try:
            # 下载图片
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(image_url)
                response.raise_for_status()
                content = response.content
            
            # 检查文件大小（10MB限制）
            MAX_FILE_SIZE = 10 * 1024 * 1024
            if len(content) > MAX_FILE_SIZE:
                print(f"    ⚠ 图片过大，跳过: {image_url}")
                return None
            
            # 验证图片格式
            try:
                image = Image.open(io.BytesIO(content))
                image.verify()
            except Exception as e:
                print(f"    ⚠ 无效的图片格式，跳过: {str(e)}")
                return None
            
            # 重新打开图片（verify后需要重新打开）
            image = Image.open(io.BytesIO(content))
            
            # 压缩图片（如果太大）
            max_size = (2000, 2000)
            if image.size[0] > max_size[0] or image.size[1] > max_size[1]:
                image.thumbnail(max_size, Image.Resampling.LANCZOS)
            
            # 确定文件扩展名
            content_type = response.headers.get("content-type", "")
            if "jpeg" in content_type or "jpg" in content_type:
                file_ext = ".jpg"
            elif "png" in content_type:
                file_ext = ".png"
            elif "webp" in content_type:
                file_ext = ".webp"
            elif "gif" in content_type:
                file_ext = ".gif"
            else:
                # 尝试从URL中获取扩展名
                from urllib.parse import urlparse
                parsed = urlparse(image_url)
                path_ext = Path(parsed.path).suffix.lower()
                if path_ext in [".jpg", ".jpeg", ".png", ".webp", ".gif"]:
                    file_ext = path_ext
                else:
                    file_ext = ".jpg"  # 默认使用jpg
            
            # 生成文件名（使用食物名称的哈希和UUID）
            food_name_hash = str(hash(food_name))[:8]
            filename = f"{food_name_hash}_{uuid.uuid4().hex[:8]}{file_ext}"
            
            # 获取存储路径（始终基于项目根目录 backend/）
            # 获取脚本所在目录的父目录（backend/）
            backend_dir = Path(__file__).parent.parent  # backend/data_init -> backend/
            
            # 确保路径基于 backend/ 目录，避免在根目录创建文件夹
            if CONFIG_AVAILABLE:
                # 使用配置中的路径，但确保基于项目根目录
                base_path_str = settings.IMAGE_STORAGE_PATH
                if Path(base_path_str).is_absolute():
                    base_path = Path(base_path_str)
                else:
                    # 确保路径基于 backend/ 目录
                    base_path = backend_dir / base_path_str
                storage_path = base_path / "food_images"
            else:
                # 回退到默认路径（确保基于 backend/ 目录）
                storage_path = backend_dir / "uploads" / "food_images"
            
            # 确保 storage_path 是 backend/ 的子目录
            try:
                storage_path.relative_to(backend_dir)
            except ValueError:
                # 如果路径不在 backend/ 目录下，强制使用 backend/uploads/food_images
                storage_path = backend_dir / "uploads" / "food_images"
            
            # 只在需要时创建文件夹（确保路径正确）
            if not storage_path.exists():
                storage_path.mkdir(parents=True, exist_ok=True)
            
            file_path = storage_path / filename
            
            # 保存图片
            image.save(file_path, quality=85, optimize=True)
            
            # 生成本地访问URL
            relative_path = f"food_images/{filename}"
            if CONFIG_AVAILABLE:
                local_url = get_image_url(relative_path)
            else:
                # 回退到默认URL格式
                local_url = f"http://localhost:8000/static/{relative_path}"
            
            return local_url
            
        except httpx.HTTPError as e:
            print(f"    ⚠ 下载图片失败: {str(e)}")
            return None
        except Exception as e:
            print(f"    ⚠ 保存图片失败: {str(e)}")
            return None
    
    async def insert_foods(self, foods: List[Dict[str, Any]], skip_existing: bool = False):
        """
        插入食物数据
        
        Args:
            foods: 食物数据列表（每个食物必须包含 image_url 字段）
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
        missing_image_count = 0
        
        if skip_existing:
            # 获取已存在的食物名称
            existing_names = set(await self.get_existing_foods())
            print(f"  数据库中已有 {len(existing_names)} 个食物")
        
        for food in foods:
            try:
                food_name = food.get('name', '未命名')
                original_image_url = food.get('image_url')
                
                # 检查是否有 image_url
                if not original_image_url:
                    missing_image_count += 1
                    print(f"  ⚠ 警告: '{food_name}' 缺少 image_url 字段")
                else:
                    # 如果是外部URL，下载并保存到本地
                    if original_image_url.startswith(("http://", "https://")):
                        print(f"  📥 正在下载图片: {food_name}...", end='', flush=True)
                        local_image_url = await self.download_and_save_image(original_image_url, food_name)
                        if local_image_url:
                            food['image_url'] = local_image_url
                            print(f" ✓")
                        else:
                            # 下载失败，保留原始URL
                            print(f" ✗ (保留原始URL)")
                    # 如果已经是本地URL，直接使用
                    else:
                        pass  # 保持原样
                
                # 如果需要跳过已存在的食物
                if skip_existing and food_name in existing_names:
                    skip_count += 1
                    print(f"  ⊘ 跳过已存在的食物: {food_name}")
                    continue
                
                # 插入食物
                await self.db.foods.insert_one(food)
                success_count += 1
                image_status = "📷" if food.get('image_url') else "⚠"
                print(f"  ✓ 已插入: {food_name} ({food.get('category', '未分类')}) {image_status}")
                
            except Exception as e:
                error_count += 1
                print(f"  ✗ 插入失败: {food.get('name', '未知')} - {e}")
        
        print(f"\n插入结果:")
        print(f"  成功: {success_count} 个")
        if skip_count > 0:
            print(f"  跳过: {skip_count} 个")
        if error_count > 0:
            print(f"  失败: {error_count} 个")
        if missing_image_count > 0:
            print(f"  ⚠ 缺少 image_url: {missing_image_count} 个")
    
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
            print("\n⚠ 警告: 覆盖模式 - 将删除所有现有食物数据及其关联的图片文件")
            response = input("确认继续? (yes/no): ").strip().lower()
            if response != 'yes':
                print("✗ 操作已取消")
                return False
            
            if not await self.clear_collection("foods", delete_images=True):
                return False
        
        # 4. 插入食物数据
        print(f"\n开始插入 {len(foods)} 个食物...")
        print("-" * 80)
        
        await self.insert_foods(
            foods, 
            skip_existing=skip_existing and not overwrite
        )
        
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

