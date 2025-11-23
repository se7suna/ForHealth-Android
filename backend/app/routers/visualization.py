"""
可视化报告 API
提供数据分析和可视化相关的端点
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import JSONResponse
from typing import List, Optional, Dict, Any
from datetime import date, datetime, timedelta
from pydantic import BaseModel
from bson import ObjectId

from app.utils.auth import get_current_user
from app.models.user import User
from app.models.food import FoodRecord
from app.models.sports import SportsRecord
from app.database import db

router = APIRouter(prefix="/api/visualization", tags=["可视化报告"])


# ============ 数据模型 ============

class DailyCalorieSummary(BaseModel):
    """每日卡路里摘要"""
    date: str  # YYYY-MM-DD
    total_intake: float  # 总摄入卡路里
    total_burned: float  # 总消耗卡路里
    daily_goal: float  # 每日卡路里目标
    net_calories: float  # 净卡路里（摄入 - 消耗）
    goal_percentage: float  # 摄入占目标的百分比
    is_over_budget: bool  # 是否超预算


class NutritionRatio(BaseModel):
    """营养素比例"""
    protein: float  # 蛋白质占比
    carbohydrates: float  # 碳水化合物占比
    fat: float  # 脂肪占比


class NutritionIntakeVsRecommended(BaseModel):
    """营养素摄入vs推荐量"""
    nutrient_name: str  # 营养素名称
    actual: float  # 实际摄入量
    recommended: float  # 推荐量
    percentage: float  # 实际/推荐的百分比


class FoodCategoryDistribution(BaseModel):
    """食物类别分布"""
    category: str  # 类别名称
    count: int  # 记录数量
    total_calories: float  # 总卡路里
    percentage: float  # 占比


class NutritionAnalysisResponse(BaseModel):
    """营养素分析响应"""
    date_range: dict  # {start_date, end_date}
    macronutrient_ratio: NutritionRatio  # 宏量营养素比例
    nutrition_vs_recommended: List[NutritionIntakeVsRecommended]  # 营养素对比
    food_category_distribution: List[FoodCategoryDistribution]  # 食物类别分布


class TimeSeriesDataPoint(BaseModel):
    """时间序列数据点"""
    date: str  # YYYY-MM-DD
    value: float  # 数值


class TimeSeriesTrendResponse(BaseModel):
    """时间序列趋势响应"""
    view_type: str  # day, week, month
    date_range: dict  # {start_date, end_date}
    intake_trend: List[TimeSeriesDataPoint]  # 摄入趋势
    burned_trend: List[TimeSeriesDataPoint]  # 消耗趋势
    weight_trend: List[TimeSeriesDataPoint]  # 体重趋势


class HealthReportExportResponse(BaseModel):
    """健康报告导出响应"""
    user_info: Dict[str, Any]  # 用户信息
    date_range: dict  # {start_date, end_date}
    summary: Dict[str, Any]  # 总体摘要
    daily_calorie_summary: DailyCalorieSummary  # 每日卡路里摘要（最新日期）
    nutrition_analysis: NutritionAnalysisResponse  # 营养素分析
    time_series_trend: TimeSeriesTrendResponse  # 时间序列趋势
    generated_at: str  # 报告生成时间


# ============ API 端点 ============

@router.get("/daily-calorie-summary", response_model=DailyCalorieSummary)
async def get_daily_calorie_summary(
    target_date: date = Query(default=None, description="目标日期(YYYY-MM-DD),默认为今天"),
    current_user: User = Depends(get_current_user)
):
    """
    获取每日卡路里摘要

    Issue #22: 可视化报告：每日卡路里摘要

    返回指定日期的：
    - 总摄入卡路里（来自饮食记录）
    - 总消耗卡路里（来自运动记录）
    - 每日卡路里目标（来自用户配置）
    - 净卡路里和目标完成百分比
    """
    if target_date is None:
        target_date = date.today()

    date_str = target_date.isoformat()

    # 获取用户的每日卡路里目标
    user_doc = await db.users.find_one({"email": current_user.email})
    if not user_doc:
        raise HTTPException(status_code=404, detail="用户不存在")

    daily_goal = user_doc.get("daily_calorie_goal", 2000.0)  # 默认2000

    # 计算日期范围（当天0点到23:59:59）
    start_datetime = datetime.combine(target_date, datetime.min.time())
    end_datetime = datetime.combine(target_date, datetime.max.time())

    # 聚合饮食记录的卡路里摄入
    food_pipeline = [
        {
            "$match": {
                "user_email": current_user.email,
                "recorded_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$group": {
                "_id": None,
                "total_intake": {"$sum": "$nutrition_data.calories"}
            }
        }
    ]

    food_result = await db.food_records.aggregate(food_pipeline).to_list(length=1)
    total_intake = food_result[0]["total_intake"] if food_result else 0.0

    # 聚合运动记录的卡路里消耗
    sports_pipeline = [
        {
            "$match": {
                "email": current_user.email,
                "created_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$group": {
                "_id": None,
                "total_burned": {"$sum": "$calories_burned"}
            }
        }
    ]

    sports_result = await db.sports_records.aggregate(sports_pipeline).to_list(length=1)
    total_burned = sports_result[0]["total_burned"] if sports_result else 0.0

    # 计算净卡路里和百分比
    net_calories = total_intake - total_burned
    goal_percentage = (total_intake / daily_goal * 100) if daily_goal > 0 else 0
    is_over_budget = total_intake > daily_goal

    return DailyCalorieSummary(
        date=date_str,
        total_intake=round(total_intake, 2),
        total_burned=round(total_burned, 2),
        daily_goal=round(daily_goal, 2),
        net_calories=round(net_calories, 2),
        goal_percentage=round(goal_percentage, 2),
        is_over_budget=is_over_budget
    )


@router.get("/nutrition-analysis", response_model=NutritionAnalysisResponse)
async def get_nutrition_analysis(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    current_user: User = Depends(get_current_user)
):
    """
    获取营养素与食物来源分析

    Issue #25: 可视化报告：营养素与食物来源分析

    返回指定日期范围内的：
    - 宏量营养素比例（蛋白质、碳水、脂肪）
    - 各营养素摄入量vs推荐量
    - 食物类别分布
    """
    if start_date > end_date:
        raise HTTPException(status_code=400, detail="开始日期不能晚于结束日期")

    # 计算日期时间范围
    start_datetime = datetime.combine(start_date, datetime.min.time())
    end_datetime = datetime.combine(end_date, datetime.max.time())

    # 聚合宏量营养素数据
    macro_pipeline = [
        {
            "$match": {
                "user_email": current_user.email,
                "recorded_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$group": {
                "_id": None,
                "total_protein": {"$sum": "$nutrition_data.protein"},
                "total_carbs": {"$sum": "$nutrition_data.carbohydrates"},
                "total_fat": {"$sum": "$nutrition_data.fat"},
                "total_calories": {"$sum": "$nutrition_data.calories"}
            }
        }
    ]

    macro_result = await db.food_records.aggregate(macro_pipeline).to_list(length=1)

    if not macro_result:
        # 没有数据
        return NutritionAnalysisResponse(
            date_range={"start_date": start_date.isoformat(), "end_date": end_date.isoformat()},
            macronutrient_ratio=NutritionRatio(protein=0, carbohydrates=0, fat=0),
            nutrition_vs_recommended=[],
            food_category_distribution=[]
        )

    macro_data = macro_result[0]
    total_protein = macro_data.get("total_protein", 0)
    total_carbs = macro_data.get("total_carbs", 0)
    total_fat = macro_data.get("total_fat", 0)

    # 计算宏量营养素卡路里（蛋白质和碳水4 kcal/g，脂肪9 kcal/g）
    protein_cal = total_protein * 4
    carbs_cal = total_carbs * 4
    fat_cal = total_fat * 9
    total_macro_cal = protein_cal + carbs_cal + fat_cal

    # 计算比例
    if total_macro_cal > 0:
        protein_ratio = protein_cal / total_macro_cal * 100
        carbs_ratio = carbs_cal / total_macro_cal * 100
        fat_ratio = fat_cal / total_macro_cal * 100
    else:
        protein_ratio = carbs_ratio = fat_ratio = 0

    # 获取用户信息计算推荐量
    user_doc = await db.users.find_one({"email": current_user.email})
    weight = user_doc.get("weight", 70)  # 默认70kg

    # 推荐量（简化版）
    # 蛋白质: 0.8-1.5g/kg体重
    # 碳水: 3-5g/kg体重
    # 脂肪: 0.8-1.2g/kg体重
    days_count = (end_date - start_date).days + 1

    protein_recommended = 1.2 * weight * days_count
    carbs_recommended = 4 * weight * days_count
    fat_recommended = 1.0 * weight * days_count

    nutrition_vs_recommended = [
        NutritionIntakeVsRecommended(
            nutrient_name="蛋白质",
            actual=round(total_protein, 2),
            recommended=round(protein_recommended, 2),
            percentage=round(total_protein / protein_recommended * 100, 2) if protein_recommended > 0 else 0
        ),
        NutritionIntakeVsRecommended(
            nutrient_name="碳水化合物",
            actual=round(total_carbs, 2),
            recommended=round(carbs_recommended, 2),
            percentage=round(total_carbs / carbs_recommended * 100, 2) if carbs_recommended > 0 else 0
        ),
        NutritionIntakeVsRecommended(
            nutrient_name="脂肪",
            actual=round(total_fat, 2),
            recommended=round(fat_recommended, 2),
            percentage=round(total_fat / fat_recommended * 100, 2) if fat_recommended > 0 else 0
        )
    ]

    # 聚合食物类别分布
    category_pipeline = [
        {
            "$match": {
                "user_email": current_user.email,
                "recorded_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$lookup": {
                "from": "foods",
                "localField": "food_id",
                "foreignField": "_id",
                "as": "food_info"
            }
        },
        {
            "$unwind": {
                "path": "$food_info",
                "preserveNullAndEmptyArrays": True
            }
        },
        {
            "$group": {
                "_id": {"$ifNull": ["$food_info.category", "未分类"]},
                "count": {"$sum": 1},
                "total_calories": {"$sum": "$nutrition_data.calories"}
            }
        },
        {
            "$sort": {"total_calories": -1}
        }
    ]

    category_result = await db.food_records.aggregate(category_pipeline).to_list(length=None)

    # 计算总卡路里用于百分比
    total_cal_for_percent = sum(item["total_calories"] for item in category_result)

    food_category_distribution = [
        FoodCategoryDistribution(
            category=item["_id"],
            count=item["count"],
            total_calories=round(item["total_calories"], 2),
            percentage=round(item["total_calories"] / total_cal_for_percent * 100, 2) if total_cal_for_percent > 0 else 0
        )
        for item in category_result
    ]

    return NutritionAnalysisResponse(
        date_range={"start_date": start_date.isoformat(), "end_date": end_date.isoformat()},
        macronutrient_ratio=NutritionRatio(
            protein=round(protein_ratio, 2),
            carbohydrates=round(carbs_ratio, 2),
            fat=round(fat_ratio, 2)
        ),
        nutrition_vs_recommended=nutrition_vs_recommended,
        food_category_distribution=food_category_distribution
    )


@router.get("/time-series-trend", response_model=TimeSeriesTrendResponse)
async def get_time_series_trend(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    view_type: str = Query("day", description="视图类型: day, week, month"),
    current_user: User = Depends(get_current_user)
):
    """
    获取时间序列趋势分析

    Issue #26: 可视化报告：时间序列趋势分析

    返回指定日期范围内的：
    - 卡路里摄入趋势
    - 卡路里消耗趋势
    - 体重变化趋势

    支持三种视图：
    - day: 每日数据
    - week: 每周聚合数据
    - month: 每月聚合数据
    """
    if start_date > end_date:
        raise HTTPException(status_code=400, detail="开始日期不能晚于结束日期")

    if view_type not in ["day", "week", "month"]:
        raise HTTPException(status_code=400, detail="视图类型必须是 day, week 或 month")

    # 计算日期时间范围
    start_datetime = datetime.combine(start_date, datetime.min.time())
    end_datetime = datetime.combine(end_date, datetime.max.time())

    # 根据视图类型设置分组格式
    if view_type == "day":
        date_format = "%Y-%m-%d"
    elif view_type == "week":
        date_format = "%Y-W%U"  # 年-周数
    else:  # month
        date_format = "%Y-%m"

    # 聚合饮食摄入趋势
    intake_pipeline = [
        {
            "$match": {
                "user_email": current_user.email,
                "recorded_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$group": {
                "_id": {
                    "$dateToString": {
                        "format": date_format,
                        "date": "$recorded_at"
                    }
                },
                "total_intake": {"$sum": "$nutrition_data.calories"}
            }
        },
        {
            "$sort": {"_id": 1}
        }
    ]

    intake_result = await db.food_records.aggregate(intake_pipeline).to_list(length=None)
    intake_trend = [
        TimeSeriesDataPoint(date=item["_id"], value=round(item["total_intake"], 2))
        for item in intake_result
    ]

    # 聚合运动消耗趋势
    burned_pipeline = [
        {
            "$match": {
                "email": current_user.email,
                "created_at": {"$gte": start_datetime, "$lte": end_datetime}
            }
        },
        {
            "$group": {
                "_id": {
                    "$dateToString": {
                        "format": date_format,
                        "date": "$created_at"
                    }
                },
                "total_burned": {"$sum": "$calories_burned"}
            }
        },
        {
            "$sort": {"_id": 1}
        }
    ]

    burned_result = await db.sports_records.aggregate(burned_pipeline).to_list(length=None)
    burned_trend = [
        TimeSeriesDataPoint(date=item["_id"], value=round(item["total_burned"], 2))
        for item in burned_result
    ]

    # 体重趋势（从用户更新记录或专门的体重记录表）
    # 注意：当前数据库中可能没有历史体重记录，这里返回当前体重作为示例
    user_doc = await db.users.find_one({"email": current_user.email})
    current_weight = user_doc.get("weight", 0)

    # TODO: 如果有历史体重记录表，从那里获取
    # 现在简化处理：只返回当前体重
    weight_trend = [
        TimeSeriesDataPoint(date=end_date.isoformat(), value=current_weight)
    ]

    return TimeSeriesTrendResponse(
        view_type=view_type,
        date_range={"start_date": start_date.isoformat(), "end_date": end_date.isoformat()},
        intake_trend=intake_trend,
        burned_trend=burned_trend,
        weight_trend=weight_trend
    )


@router.get("/export-report", response_model=HealthReportExportResponse)
async def export_health_report(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    current_user: User = Depends(get_current_user)
):
    """
    导出健康数据报告

    Issue #23: 可视化报告：报告导出

    返回指定日期范围内的完整健康报告数据，包括：
    - 用户基本信息
    - 每日卡路里摘要（最新日期）
    - 营养素分析
    - 时间序列趋势分析
    - 总体摘要统计

    前端可以使用这些数据生成 PDF 或长图报告
    """
    if start_date > end_date:
        raise HTTPException(status_code=400, detail="开始日期不能晚于结束日期")

    # 获取用户信息
    user_doc = await db.users.find_one({"email": current_user.email})
    if not user_doc:
        raise HTTPException(status_code=404, detail="用户不存在")

    user_info = {
        "username": user_doc.get("username"),
        "email": user_doc.get("email"),
        "age": user_doc.get("age"),
        "gender": user_doc.get("gender"),
        "height": user_doc.get("height"),
        "weight": user_doc.get("weight"),
        "health_goal_type": user_doc.get("health_goal_type"),
        "daily_calorie_goal": user_doc.get("daily_calorie_goal")
    }

    # 获取最新日期的每日卡路里摘要
    daily_summary = await get_daily_calorie_summary(
        target_date=end_date,
        current_user=current_user
    )

    # 获取营养素分析
    nutrition_analysis = await get_nutrition_analysis(
        start_date=start_date,
        end_date=end_date,
        current_user=current_user
    )

    # 获取时间序列趋势（默认按天）
    time_series = await get_time_series_trend(
        start_date=start_date,
        end_date=end_date,
        view_type="day",
        current_user=current_user
    )

    # 计算总体摘要
    start_datetime = datetime.combine(start_date, datetime.min.time())
    end_datetime = datetime.combine(end_date, datetime.max.time())

    # 统计总记录数
    total_food_records = await db.food_records.count_documents({
        "user_email": current_user.email,
        "recorded_at": {"$gte": start_datetime, "$lte": end_datetime}
    })

    total_sports_records = await db.sports_records.count_documents({
        "email": current_user.email,
        "created_at": {"$gte": start_datetime, "$lte": end_datetime}
    })

    # 计算总摄入和总消耗
    total_intake = sum(point.value for point in time_series.intake_trend)
    total_burned = sum(point.value for point in time_series.burned_trend)

    summary = {
        "days_count": (end_date - start_date).days + 1,
        "total_food_records": total_food_records,
        "total_sports_records": total_sports_records,
        "total_intake_calories": round(total_intake, 2),
        "total_burned_calories": round(total_burned, 2),
        "average_daily_intake": round(total_intake / ((end_date - start_date).days + 1), 2),
        "average_daily_burned": round(total_burned / ((end_date - start_date).days + 1), 2)
    }

    return HealthReportExportResponse(
        user_info=user_info,
        date_range={"start_date": start_date.isoformat(), "end_date": end_date.isoformat()},
        summary=summary,
        daily_calorie_summary=daily_summary,
        nutrition_analysis=nutrition_analysis,
        time_series_trend=time_series,
        generated_at=datetime.now().isoformat()
    )
