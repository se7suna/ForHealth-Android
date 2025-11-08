from pydantic import BaseModel, Field, validator
from typing import Optional
from datetime import datetime,date

# 记录运动及消耗卡路里的请求
class LogSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    crearted_at: datetime = Field(default_factory=datetime.utcnow)
    duration_time: Optional[int] = Field(None, gt=0)

# class LogSportsResponse(BaseModel): 为通用消息响应

# 创建运动类型的请求
class CreateSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    METs: Optional[int] = Field(None, gt=0)

# class CreateSportsResponse(BaseModel): 为通用消息响应

# 更新自定义运动类型的请求
class UpdateSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    METs: Optional[int] = Field(None, gt=0)

# class UpdateSportsResponse(BaseModel): 为通用消息响应

# 搜索运动类型的请求
class SearchSportsRequest(BaseModel):
    sport_type: Optional[str] = None

class SearchSportsResponse(BaseModel):
    sport_type: Optional[str] = None
    METs: Optional[int] = None

# 查看运动历史的请求
class HistorySportsRequest(BaseModel):
    start_date: Optional[date] = None
    end_date: Optional[date] = None

    @validator('end_date')# 个人认为错误处理实际应该正在service或router里进行
    def check_dates(cls, v, values):
        start_date = values.get('start_date')
        if start_date and v and v < start_date:
            raise ValueError('end_date must be after start_date')
        return v

class HistorySportsResponse(BaseModel):
    sport_type: Optional[str] = None
    crearted_at: Optional[datetime] = None
    duration_time: Optional[int] = None
    calories_burned: Optional[float] = None

class SimpleSportsResponse(BaseModel):
    success: bool
    message: str