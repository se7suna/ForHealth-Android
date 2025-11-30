from typing import Optional, List
from datetime import datetime, date
from bson import ObjectId
from app.database import get_database
from app.models.weight import WeightRecordInDB
from app.schemas.weight import WeightRecordCreateRequest, WeightRecordUpdateRequest


async def create_weight_record(
    user_email: str,
    record_data: WeightRecordCreateRequest
) -> dict:
    """创建体重记录"""
    db = get_database()

    weight_record = WeightRecordInDB(
        user_email=user_email,
        weight=record_data.weight,
        recorded_at=record_data.recorded_at,
        notes=record_data.notes
    )

    result = await db.weight_records.insert_one(weight_record.dict())

    created_record = await db.weight_records.find_one({"_id": result.inserted_id})
    created_record["_id"] = str(created_record["_id"])

    return created_record


async def get_weight_records(
    user_email: str,
    start_date: Optional[date] = None,
    end_date: Optional[date] = None,
    limit: int = 100
) -> tuple[List[dict], int]:
    """获取体重记录列表"""
    db = get_database()

    # 构建查询条件
    query = {"user_email": user_email}

    # 日期范围筛选
    if start_date or end_date:
        query["recorded_at"] = {}
        if start_date:
            start_datetime = datetime.combine(start_date, datetime.min.time())
            query["recorded_at"]["$gte"] = start_datetime
        if end_date:
            end_datetime = datetime.combine(end_date, datetime.max.time())
            query["recorded_at"]["$lte"] = end_datetime

    # 查询记录
    total = await db.weight_records.count_documents(query)
    records = await db.weight_records.find(query).sort("recorded_at", -1).limit(limit).to_list(length=limit)

    # 转换 ObjectId 为字符串
    for record in records:
        record["_id"] = str(record["_id"])

    return records, total


async def get_weight_record_by_id(record_id: str, user_email: str) -> Optional[dict]:
    """根据ID获取体重记录"""
    db = get_database()

    try:
        record = await db.weight_records.find_one({
            "_id": ObjectId(record_id),
            "user_email": user_email
        })

        if record:
            record["_id"] = str(record["_id"])

        return record
    except Exception:
        return None


async def update_weight_record(
    record_id: str,
    user_email: str,
    update_data: WeightRecordUpdateRequest
) -> Optional[dict]:
    """更新体重记录"""
    db = get_database()

    # 只更新提供的字段
    update_fields = {
        k: v for k, v in update_data.dict(exclude_unset=True).items() if v is not None
    }

    if not update_fields:
        return await get_weight_record_by_id(record_id, user_email)

    try:
        result = await db.weight_records.find_one_and_update(
            {"_id": ObjectId(record_id), "user_email": user_email},
            {"$set": update_fields},
            return_document=True
        )

        if result:
            result["_id"] = str(result["_id"])

        return result
    except Exception:
        return None


async def delete_weight_record(record_id: str, user_email: str) -> bool:
    """删除体重记录"""
    db = get_database()

    try:
        result = await db.weight_records.delete_one({
            "_id": ObjectId(record_id),
            "user_email": user_email
        })

        return result.deleted_count > 0
    except Exception:
        return False
