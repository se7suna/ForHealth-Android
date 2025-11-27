from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field

# 是否要用optional
"""数据库中的运动类型表"""
class SportsTypeInDB(BaseModel):
    created_by: str
    sport_type: str
    sport_name: Optional[str] = None
    describe: str
    METs: float
    image_url: Optional[str] = None
   

"""数据库中的运动记录"""
class SportsLogInDB(BaseModel):
    created_by: str
    sport_type: str
    sport_name: str
    created_at: datetime
    duration_time: int
    calories_burned: float