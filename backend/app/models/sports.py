from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field

"""数据库中的运动类型表"""
class SportsTypeInDB(BaseModel):
    
    email: str
    sport_type: str
    describe: str
    METs: float
   

"""数据库中的运动记录"""
class SportsLogInDB(BaseModel):
    email: str
    sport_type: str
    created_at: datetime
    duration_time: int
    calories_burned: float