from pydantic import BaseModel, Field, validator
from typing import Optional
from datetime import datetime,date

# 记录运动及消耗卡路里的请求
class LogSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    duration_time: Optional[int] = Field(None, gt=0)

# 更新运动记录的请求
class UpdateSportsRecordRequest(BaseModel):
    record_id: Optional[str] = None
    sport_type: Optional[str] = None
    created_at: Optional[datetime] = None
    duration_time: Optional[int] = Field(None, gt=0)

# 创建运动类型的请求
class CreateSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    describe: Optional[str] = None
    METs: Optional[float] = Field(None, gt=0)

# 更新自定义运动类型的请求
class UpdateSportsRequest(BaseModel):
    sport_type: Optional[str] = None
    describe: Optional[str] = None
    METs: Optional[float] = Field(None, gt=0)

# 搜索运动类型的请求
class SearchSportsRequest(BaseModel):
    sport_type: Optional[str] = None

class SearchSportsResponse(BaseModel):
    sport_type: Optional[str] = None
    describe: Optional[str] = None
    METs: Optional[float] = None

class SearchSportsResponse(BaseModel):
    sport_type: Optional[str] = None
    describe: Optional[str] = None
    METs: Optional[float] = None

# 搜索运动记录的请求
class SearchSportRecordsRequest(BaseModel):
    sport_type: Optional[str] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None

    @validator('end_date')# 个人认为错误处理实际应该正在service或router里进行
    def check_dates(cls, v, values):
        start_date = values.get('start_date')
        if start_date and v and v < start_date:
            raise ValueError('end_date must be after start_date')
        return v

class SearchSportRecordsResponse(BaseModel):
    sport_type: Optional[str] = None
    created_at: Optional[datetime] = None
    duration_time: Optional[int] = None
    calories_burned: Optional[float] = None
    record_id: Optional[str] = None

class SimpleSportsResponse(BaseModel):
    success: bool
    message: str