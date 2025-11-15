from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field

# 用email做用户唯一凭证
default_email="sport@default.com"# 表示公用运动类型

DefaultSports = (
        {"sport_type": "跑步", "METs": 8,"describe":"高强度有氧运动，有效提升心肺功能和燃烧卡路里，适合大多数健康成年人","email":default_email},
        {"sport_type": "游泳", "METs": 6,"describe":"低冲击性全身运动，锻炼几乎所有肌肉群，对关节友好，适合各年龄段人群","email":default_email},
        {"sport_type": "骑自行车", "METs": 7,"describe":"中等至高强度有氧运动，主要锻炼下肢肌肉，提升心肺耐力，可调节强度适应不同体能水平","email":default_email},
        {"sport_type": "散步", "METs": 3.5,"describe":"低强度有氧运动，适合初学者或恢复期人群，有助于改善心血管健康和日常活动能力","email":default_email},
)

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