from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field
from enum import Enum

# 用email做用户唯一凭证
default_email="sport@default.com"# 表示公用运动类型

DefaultSports = (
        {"sport_type": "跑步", "METs": 8,"email":default_email},
        {"sport_type": "游泳", "METs": 6,"email":default_email},
        {"sport_type": "骑自行车", "METs": 7,"email":default_email},
        {"sport_type": "散步", "METs": 3.5,"email":default_email},
)

"""数据库中的运动类型表"""
class SportsTypeInDB(BaseModel):
    email: str
    sport_type: str
    METs: int
   

"""数据库中的运动记录"""
class SportsLogInDB(BaseModel):
    email: str
    sport_type: str
    crearted_at: datetime
    duration_time: int
    calories_burned: float