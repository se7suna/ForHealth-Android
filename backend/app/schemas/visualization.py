"""
可视化报告相关的数据模型
"""
from typing import List, Dict, Any
from pydantic import BaseModel


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
