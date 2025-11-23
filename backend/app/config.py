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

    # 初始化数据库默认内容
    ## 管理员账户用于权限写入
    DEFAULT_AUTH_EMAIL: str = "user@example.com"
    DEFAULT_PASSWORD: str = "123456"
    ## 普通账户用于认证登陆
    USER_EMAIL: str = "test@user.com"
    USER_PASSWORD: str = "test1234"
    ## 默认运动类型，用于记录运动
    DEFAULT_SPORT_EMAIL: str = "user@example.com"# 表示公用类型，管理员具有写入权限
    DefaultSports :tuple = (
        {"sport_type": "跑步", "METs": 8,"describe":"高强度有氧运动，有效提升心肺功能和燃烧卡路里，适合大多数健康成年人","email":DEFAULT_SPORT_EMAIL},
        {"sport_type": "游泳", "METs": 6,"describe":"低冲击性全身运动，锻炼几乎所有肌肉群，对关节友好，适合各年龄段人群","email":DEFAULT_SPORT_EMAIL},
        {"sport_type": "骑自行车", "METs": 7,"describe":"中等至高强度有氧运动，主要锻炼下肢肌肉，提升心肺耐力，可调节强度适应不同体能水平","email":DEFAULT_SPORT_EMAIL},
        {"sport_type": "散步", "METs": 3.5,"describe":"低强度有氧运动，适合初学者或恢复期人群，有助于改善心血管健康和日常活动能力","email":DEFAULT_SPORT_EMAIL},
    )

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
