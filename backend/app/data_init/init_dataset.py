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
    
# 初始化运动表，填入默认运动类型和卡路里消耗
from app.config import settings
from app.database import get_database
async def initialize_sports_table():
    db = get_database()
    for sport in settings.DefaultSports:
        existing = await db["sports"].find_one({"sport_type": sport["sport_type"],"email": settings.DEFAULT_SPORT_EMAIL})
        if not existing:
            await db["sports"].insert_one(sport)


# 初始化食物表，填入默认食物数据
import json
import httpx
import uuid
import io
from pathlib import Path
from typing import Optional
from datetime import datetime
from PIL import Image
from app.utils.image_storage import get_image_url


async def _download_and_save_food_image(image_url: str, food_name: str) -> Optional[str]:
    """
    从外部URL下载图片并保存到本地文件系统（与init_dataset.py中的逻辑一致）
    
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
            return None
        
        # 验证图片格式
        try:
            image = Image.open(io.BytesIO(content))
            image.verify()
        except Exception:
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
        
        # 获取存储路径（基于项目根目录 backend/）
        app_dir = Path(__file__).parent  # backend/app
        backend_dir = app_dir.parent  # backend/
        base_path_str = settings.IMAGE_STORAGE_PATH
        if Path(base_path_str).is_absolute():
            base_path = Path(base_path_str)
        else:
            base_path = backend_dir / base_path_str
        storage_path = base_path / "food_images"
        
        # 确保 storage_path 是 backend/ 的子目录
        try:
            storage_path.relative_to(backend_dir)
        except ValueError:
            storage_path = backend_dir / "uploads" / "food_images"
        
        # 创建文件夹（如果不存在）
        if not storage_path.exists():
            storage_path.mkdir(parents=True, exist_ok=True)
        
        file_path = storage_path / filename
        
        # 保存图片
        image.save(file_path, quality=85, optimize=True)
        
        # 生成本地访问URL
        relative_path = f"food_images/{filename}"
        local_url = get_image_url(relative_path)
        
        return local_url
        
    except (httpx.HTTPError, Exception):
        return None


async def initialize_foods_table():
    """初始化食物表，从JSON文件加载默认食物数据并下载图片"""
    print("开始初始化食物表")
    db = get_database()
    
    # 加载数据集文件
    app_dir = Path(__file__).parent  # backend/app
    backend_dir = app_dir.parent  # backend/
    dataset_path = backend_dir / "data_init" / "initial_foods_dataset.json"
    
    if not dataset_path.exists():
        return
    
    try:
        with open(dataset_path, 'r', encoding='utf-8') as f:
            dataset = json.load(f)
    except Exception:
        return
    
    foods = dataset.get('foods', [])
    if not foods:
        return
    
    # 添加时间戳
    now = datetime.utcnow()
    
    for food in foods:
        try:
            food_name = food.get('name', '未命名')
            original_image_url = food.get('image_url')
            
            # 检查是否已存在（通过 name 和 created_by="all"）
            existing = await db["foods"].find_one({
                "name": food_name,
                "created_by": "all"
            })
            
            if existing:
                continue
            
            # 处理图片：如果是外部URL，下载并保存到本地
            if original_image_url and original_image_url.startswith(("http://", "https://")):
                local_image_url = await _download_and_save_food_image(original_image_url, food_name)
                if local_image_url:
                    food['image_url'] = local_image_url
            
            # 添加时间戳
            food['created_at'] = now
            food['updated_at'] = now
            
            # 确保 created_by 为 "all"（表示所有人可见）
            food['created_by'] = "all"
            
            # 插入食物
            await db["foods"].insert_one(food)
            
        except Exception:
            pass


# 初始化用户,在系统启动前，功能耦合感觉有点丑陋
from app.utils.security import get_password_hash
from app.models.user import UserInDB
from datetime import datetime
from app.models.user import Gender
from app.services.calculation_service import calculate_age,calculate_bmr
async def initialize_default_user():
    db = get_database()

    existing = await db["users"].find_one({"email": settings.DEFAULT_AUTH_EMAIL})
    if not existing:
        user_data = UserInDB(
            email=settings.DEFAULT_AUTH_EMAIL, 
            username="Default User", 
            hashed_password=get_password_hash(settings.DEFAULT_PASSWORD)
        ).dict()
        await db["users"].insert_one(user_data)
        # 更新身体数据
        update_data = {
            "height": 170.0,
            "weight": 70.0,
            "birthdate": datetime(2000, 1, 1),
            "age": calculate_age(datetime(2000, 1, 1)),
            "gender": Gender.MALE,
            "bmr": calculate_bmr(70.0,170.0,calculate_age(datetime(2000, 1, 1)),Gender.MALE),
            "updated_at": datetime.utcnow(),
        }
        await db.users.find_one_and_update(
            {"email": settings.DEFAULT_AUTH_EMAIL},
            {"$set": update_data})


    existing = None
    existing = await db["users"].find_one({"email": settings.USER_EMAIL})
    if not existing:
        user_data = UserInDB(
            email=settings.USER_EMAIL, 
            username="test_user", 
            hashed_password=get_password_hash(settings.USER_PASSWORD)
        ).dict()
        await db["users"].insert_one(user_data)
        # 更新身体数据
        update_data = {
            "height": 170.0,
            "weight": 70.0,
            "birthdate": datetime(2000, 1, 1),
            "age": calculate_age(datetime(2000, 1, 1)),
            "gender": Gender.MALE,
            "bmr": calculate_bmr(70.0,170.0,calculate_age(datetime(2000, 1, 1)),Gender.MALE),
            "updated_at": datetime.utcnow(),
        }
        await db.users.find_one_and_update(
            {"email": settings.USER_EMAIL},
            {"$set": update_data})


