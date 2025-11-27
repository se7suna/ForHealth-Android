"""
图片存储工具模块

用于处理食物图片的上传、存储和访问
"""
import io
import uuid
from pathlib import Path
from typing import Optional
from fastapi import UploadFile, HTTPException, status
from PIL import Image
from app.config import settings


# 允许的图片格式
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"}
ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif","svg"}

# 最大文件大小（10MB）
MAX_FILE_SIZE = 10 * 1024 * 1024

def get_full_image_base_url() -> str:
    """获取完整的图片访问基础URL（包含协议和主机）"""
    protocol = "https" if settings.PORT == 443 else "http"
    # 去掉 IMAGE_BASE_URL 开头的斜杠（如果有）
    base_url_path = settings.IMAGE_BASE_URL.lstrip("/")
    return f"{protocol}://{settings.HOST}:{settings.PORT}/{base_url_path}"


def get_image_storage_path() -> Path:
    """
    获取食物图片存储路径
    
    Returns:
        食物图片存储目录的Path对象（uploads/food_images）
    """
    # 从配置中获取基础存储路径，然后拼接 food_images 子文件夹
    base_path = Path(settings.IMAGE_STORAGE_PATH)
    storage_path = base_path / "food_images"
    storage_path.mkdir(parents=True, exist_ok=True)
    return storage_path


def validate_image_file(file: UploadFile) -> None:
    """
    验证图片文件
    
    Args:
        file: 上传的文件对象
        
    Raises:
        HTTPException: 如果文件格式或大小不符合要求
    """
    # 检查文件类型
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"不支持的图片格式。支持的格式：{', '.join(ALLOWED_IMAGE_TYPES)}"
        )
    
    # 检查文件扩展名
    if file.filename:
        file_ext = Path(file.filename).suffix.lower()
        if file_ext not in ALLOWED_IMAGE_EXTENSIONS:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"不支持的图片扩展名。支持的扩展名：{', '.join(ALLOWED_IMAGE_EXTENSIONS)}"
            )


async def save_food_image(file: UploadFile, food_id: Optional[str] = None) -> str:
    """
    保存食物图片文件
    
    Args:
        file: 上传的图片文件
        food_id: 食物ID（可选，用于生成文件名）
        
    Returns:
        保存后的图片相对路径（用于生成访问URL）
        
    Raises:
        HTTPException: 如果保存失败
    """
    # 验证文件
    validate_image_file(file)
    
    # 生成唯一文件名
    if food_id:
        # 如果有食物ID，使用食物ID作为文件名的一部分
        file_ext = Path(file.filename).suffix.lower() if file.filename else ".jpg"
        filename = f"{food_id}_{uuid.uuid4().hex[:8]}{file_ext}"
    else:
        # 否则使用UUID生成文件名
        file_ext = Path(file.filename).suffix.lower() if file.filename else ".jpg"
        filename = f"{uuid.uuid4().hex}{file_ext}"
    
    # 获取存储路径
    storage_path = get_image_storage_path()
    file_path = storage_path / filename
    
    try:
        # 读取文件内容
        content = await file.read()
        
        # 检查文件大小
        if len(content) > MAX_FILE_SIZE:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"图片文件过大。最大允许大小：{MAX_FILE_SIZE / 1024 / 1024}MB"
            )
        
        # 验证图片格式（使用PIL）
        try:
            image = Image.open(io.BytesIO(content))
            image.verify()  # 验证图片
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"无效的图片文件：{str(e)}"
            )
        
        # 重新打开图片（verify后需要重新打开）
        image = Image.open(io.BytesIO(content))
        
        # 可选：压缩图片（如果太大）
        # 限制最大尺寸为2000x2000
        max_size = (2000, 2000)
        if image.size[0] > max_size[0] or image.size[1] > max_size[1]:
            image.thumbnail(max_size, Image.Resampling.LANCZOS)
        
        # 保存图片
        image.save(file_path, quality=85, optimize=True)
        
        # 返回相对路径（相对于项目根目录）
        return f"food_images/{filename}"
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"保存图片失败：{str(e)}"
        )


def get_image_url(relative_path: str) -> str:
    """
    生成图片访问URL（完整URL，包含协议和主机）
    
    Args:
        relative_path: 图片相对路径（如：food_images/xxx.jpg）
        
    Returns:
        完整的图片访问URL（从配置中读取，格式：{protocol}://{host}:{port}/{IMAGE_BASE_URL}/{relative_path}）
    """
    base_url = get_full_image_base_url()
    return f"{base_url}/{relative_path}"


def delete_food_image(image_url: Optional[str]) -> bool:
    """
    删除食物图片文件
    
    Args:
        image_url: 图片URL或相对路径
        
    Returns:
        是否删除成功
    """
    if not image_url:
        return False
    
    try:
        # 从配置中获取静态文件基础路径（去掉开头的斜杠）
        static_base_path = settings.IMAGE_BASE_URL.lstrip("/")
        
        # 从URL中提取相对路径
        if image_url.startswith("http://") or image_url.startswith("https://"):
            # 如果是完整URL，提取路径部分
            from urllib.parse import urlparse
            parsed = urlparse(image_url)
            path = parsed.path.lstrip("/")
            # 去掉静态文件基础路径前缀（如果存在）
            if path.startswith(f"{static_base_path}/"):
                relative_path = path.replace(f"{static_base_path}/", "", 1)
            elif path == static_base_path:
                relative_path = ""
            else:
                relative_path = path
        elif image_url.startswith(f"/{static_base_path}/"):
            # 如果是静态文件URL
            relative_path = image_url.replace(f"/{static_base_path}/", "")
        elif image_url == f"/{static_base_path}" or image_url == f"/{static_base_path}/":
            relative_path = ""
        else:
            # 假设已经是相对路径（如：food_images/xxx.jpg）
            relative_path = image_url
        
        # 构建完整文件路径
        # 如果 relative_path 已经包含 food_images/，直接使用
        # 否则需要添加 food_images/ 前缀
        if relative_path.startswith("food_images/"):
            storage_path = get_image_storage_path().parent  # 回到uploads目录
            file_path = storage_path / relative_path
        else:
            # 如果只是文件名，需要添加 food_images/ 前缀
            storage_path = get_image_storage_path()
            file_path = storage_path / relative_path
        
        # 删除文件
        if file_path.exists() and file_path.is_file():
            file_path.unlink()
            return True
        
        return False
    except Exception as e:
        # 打印错误信息以便调试
        import traceback
        print(f"删除图片失败: {image_url}, 错误: {str(e)}")
        print(traceback.format_exc())
        return False

