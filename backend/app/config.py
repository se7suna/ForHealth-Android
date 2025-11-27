from pydantic_settings import BaseSettings
from typing import List, Union
from pydantic import field_validator


class Settings(BaseSettings):
    """应用配置"""

    # 应用配置
    APP_NAME: str = "For Health API"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True

    # 服务器配置
    HOST: str = "127.0.0.1"
    PORT: int = 8000

    # 数据库配置
    MONGODB_URL: str = "mongodb://localhost:27017"
    DATABASE_NAME: str = "for_health"

    # JWT 配置
    SECRET_KEY: str = "default-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # 邮件配置
    SMTP_HOST: str = "smtp.gmail.com"
    SMTP_PORT: int = 587
    SMTP_USER: str = "test@test.com"
    SMTP_PASSWORD: str = "test"
    SMTP_FROM_EMAIL: str = "noreply@forhealth.com"
    SMTP_FROM_NAME: str = "For Health"

    # CORS 配置
    ALLOWED_ORIGINS: Union[List[str], str] = "http://localhost:3000,http://localhost:8080"

    # 图片存储配置
    IMAGE_STORAGE_PATH: str = "uploads"  # 图片存储基础路径（相对于项目根目录），包含 food_images 和 sports_images 等子文件夹
    IMAGE_BASE_URL: str = "/static"  # 图片访问基础URL（相对路径）

    def get_full_image_base_url(self) -> str:
        """获取完整的图片访问基础URL（包含协议和主机）"""
        protocol = "https" if self.PORT == 443 else "http"
        # 去掉 IMAGE_BASE_URL 开头的斜杠（如果有）
        base_url_path = self.IMAGE_BASE_URL.lstrip("/")
        return f"{protocol}://{self.HOST}:{self.PORT}/{base_url_path}"


    ## 管理员账户 （现在与普通用户没有任何区别）
    DEFAULT_AUTH_EMAIL: str = "user@example.com"
    DEFAULT_PASSWORD: str = "123456"
    ## 普通账户用于认证登陆
    USER_EMAIL: str = "test@user.com"
    USER_PASSWORD: str = "test1234"
    
    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        """解析 CORS origins，支持逗号分隔的字符串或列表"""
        if isinstance(v, str):
            return [origin.strip() for origin in v.split(",")]
        return v

    class Config:
        env_file = ".env"
        case_sensitive = True
        extra="ignore"

settings = Settings()
