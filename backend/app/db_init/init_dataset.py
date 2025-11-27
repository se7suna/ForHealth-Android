"""
用于从数据集文件初始化食物数据库
注意：数据集文件中的每个食物必须包含 image_url 字段
图片将从外部URL下载并保存到本地文件系统
"""

import sys
from pathlib import Path
# 添加项目根目录到路径，以便导入应用模块
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    from app.config import settings
    from app.utils.image_storage import get_image_url
    CONFIG_AVAILABLE = True
except ImportError:
    CONFIG_AVAILABLE = False
    print("⚠ 警告: 无法导入配置模块，将使用默认路径")

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


# 初始化食物表，填入默认食物数据
import json
import httpx
import uuid
import io
from pathlib import Path
from typing import Optional
from datetime import datetime
from PIL import Image
from app.utils.image_storage import get_image_url,ALLOWED_IMAGE_EXTENSIONS

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
        app_dir = Path(__file__).parent.parent  # backend/app
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
    
    db = get_database()
    
    # 加载数据集文件
    app_dir = Path(__file__).parent  # backend/app
    backend_dir = app_dir.parent  # backend/
    dataset_path = backend_dir / "db_init" / "initial_foods_dataset.json"
    
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
            print(original_image_url)
            
            # 检查是否已存在（通过 name 和 created_by="all"）
            existing = await db["foods"].find_one({
                "name": food_name,
                "created_by": "all"
            })
            
            if existing:
                print("食物存在并跳过")
                continue
            
            # 处理图片：如果是外部URL，下载并保存到本地
            if original_image_url and original_image_url.startswith(("http://", "https://")):
                local_image_url = await _download_and_save_food_image(original_image_url, food_name)
                if local_image_url:
                    food['image_url'] = local_image_url
                    print(food['image_url'])
                    print(local_image_url)
                else:
                    print("图片下载或保存失败，返回为空")
            else:
                print("url不存在或者格式错误")
            
            # 添加时间戳
            food['created_at'] = now
            food['updated_at'] = now
            
            # 确保 created_by 为 "all"（表示所有人可见）
            food['created_by'] = "all"
            
            # 插入食物
            await db["foods"].insert_one(food)
            
        except Exception:
            pass


# 初始化运动表，填入默认运动类型和卡路里消耗
from app.config import settings
from app.database import get_database
from app.models.sports import SportsTypeInDB

async def _download_and_save_sport_image(image_url: str, sport_name: str) -> Optional[str]:
    """
    从外部URL下载图片并保存到本地文件系统（与init_dataset.py中的逻辑一致）
    
    Args:
        image_url: 外部图片URL
        sport_name: 食物名称（用于生成文件名）
        
    Returns:
        本地图片访问URL，如果下载失败则返回None
    """
    if not image_url or not image_url.startswith(("http://", "https://")):
        print("url不存在或者格式错误")
        return None
    # 下载图片
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(image_url)
        content = response.content
        
    # 确定文件扩展名
    from urllib.parse import urlparse
    parsed = urlparse(image_url)# 从URL中获取扩展名
    path_ext = Path(parsed.path).suffix.lower()
    file_ext = path_ext
             
    # 生成文件名（使用运动名称的哈希和UUID）
    sport_name_hash = str(hash(sport_name))[:8]
    filename = f"{sport_name_hash}_{uuid.uuid4().hex[:8]}{file_ext}"
        
    # 获取存储路径（基于项目根目录 backend/）
    backend_dir = Path(__file__).parent.parent.parent  # backend
    base_path_str = settings.IMAGE_STORAGE_PATH
    if Path(base_path_str).is_absolute():
        base_path = Path(base_path_str)
    else:
        base_path = backend_dir / base_path_str
    storage_path = base_path / "sport_images"
        
    # 确保 storage_path 是 backend/ 的子目录
    try:
        storage_path.relative_to(backend_dir)
    except ValueError:
        storage_path = backend_dir / "uploads" / "sport_images"
        
    # 创建文件夹（如果不存在）
    if not storage_path.exists():
        storage_path.mkdir(parents=True, exist_ok=True)
        
    file_path = storage_path / filename
        
    # 保存图片
    with open(file_path, "wb") as f:
        f.write(content)
        #print("图片保存成功:",file_path)
        
    # 生成本地访问URL
    relative_path = f"sport_images/{filename}"
    local_url = get_image_url(relative_path)
        
    return local_url



async def initialize_sports_table():
    
    db = get_database()

     # 加载数据集文件
    app_dir = Path(__file__).parent  # backend/app
    backend_dir = app_dir.parent  # backend/
    dataset_path = backend_dir / "db_init" / "initial_sports_dataset.json"
    
    if not dataset_path.exists():
        print("""⚠ 警告: 运动数据集文件不存在，跳过初始化运动表""")
        return
    
    try:
        with open(dataset_path, 'r', encoding='utf-8') as f:
            dataset = json.load(f)
    except Exception:
        print("""⚠ 警告: 无法加载运动数据集文件，跳过初始化运动表""")
        return
    
    for sport in dataset:
        #检查是否已存在
        existing = await db["sports"].find_one({"sport_name": sport["sport_name"]})
        if existing:
            continue
        # 处理图片：将外部URL下载并保存到本地
        if "image_url" in sport:
            local_image_url = await _download_and_save_sport_image(sport["image_url"], sport["sport_name"])
            if local_image_url:
                sport["image_url"] = local_image_url
        else:
            sport["image_url"] = ""
        await db["sports"].insert_one(SportsTypeInDB(**sport).model_dump())
    




