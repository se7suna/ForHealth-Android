from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class WeightRecordCreateRequest(BaseModel):
    """创建体重记录请求"""
    weight: float = Field(..., gt=0, le=500, description="体重（公斤，0-500kg）")
    recorded_at: datetime = Field(..., description="记录时间")
    notes: Optional[str] = Field(None, max_length=200, description="备注")

    class Config:
        json_schema_extra = {
            "example": {
                "weight": 70.5,
                "recorded_at": "2025-11-24T10:30:00",
                "notes": "晨起空腹"
            }
        }


class WeightRecordUpdateRequest(BaseModel):
    """更新体重记录请求"""
    weight: Optional[float] = Field(None, gt=0, le=500, description="体重（公斤，0-500kg）")
    recorded_at: Optional[datetime] = Field(None, description="记录时间")
    notes: Optional[str] = Field(None, max_length=200, description="备注")

    class Config:
        json_schema_extra = {
            "example": {
                "weight": 71.0,
                "recorded_at": "2025-11-25T10:30:00",
                "notes": "晨起空腹"
            }
        }


class WeightRecordResponse(BaseModel):
    """体重记录响应"""
    id: str = Field(..., description="记录ID")
    weight: float = Field(..., description="体重（公斤）")
    recorded_at: datetime = Field(..., description="记录时间")
    notes: Optional[str] = Field(None, description="备注")
    created_at: datetime = Field(..., description="创建时间")

    class Config:
        json_schema_extra = {
            "example": {
                "id": "507f1f77bcf86cd799439011",
                "weight": 70.5,
                "recorded_at": "2025-11-24T10:30:00",
                "notes": "晨起空腹",
                "created_at": "2025-11-24T10:30:00"
            }
        }


class WeightRecordListResponse(BaseModel):
    """体重记录列表响应"""
    total: int = Field(..., description="总记录数")
    records: list[WeightRecordResponse] = Field(..., description="体重记录列表")

    class Config:
        json_schema_extra = {
            "example": {
                "total": 2,
                "records": [
                    {
                        "id": "507f1f77bcf86cd799439011",
                        "weight": 70.5,
                        "recorded_at": "2025-11-24T10:30:00",
                        "notes": "晨起空腹",
                        "created_at": "2025-11-24T10:30:00"
                    }
                ]
            }
        }
