from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class WeightRecordInDB(BaseModel):
    """数据库中的体重记录模型"""
    user_email: str = Field(..., description="用户邮箱")
    weight: float = Field(..., gt=0, description="体重（公斤）")
    recorded_at: datetime = Field(..., description="记录时间")
    notes: Optional[str] = Field(None, max_length=200, description="备注")
    created_at: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        json_schema_extra = {
            "example": {
                "user_email": "user@example.com",
                "weight": 70.5,
                "recorded_at": "2025-11-24T10:30:00",
                "notes": "晨起空腹",
                "created_at": "2025-11-24T10:30:00"
            }
        }
