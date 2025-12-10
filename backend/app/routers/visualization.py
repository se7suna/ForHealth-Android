"""
可视化报告 API
提供数据分析和可视化相关的端点
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from datetime import date
from app.routers.auth import get_current_user
from app.schemas.visualization import (
    DailyCalorieSummary,
    NutritionAnalysisResponse,
    TimeSeriesTrendResponse,
    HealthReportExportResponse,
)
from app.services import visualization_service

router = APIRouter(prefix="/api/visualization", tags=["可视化报告"])


@router.get("/daily-calorie-summary", response_model=DailyCalorieSummary)
async def get_daily_calorie_summary(
    target_date: date = Query(default=None, description="目标日期(YYYY-MM-DD),默认为今天"),
    current_user: str = Depends(get_current_user)
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

    try:
        return await visualization_service.get_daily_calorie_summary(
            user_email=current_user,
            target_date=target_date
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/nutrition-analysis", response_model=NutritionAnalysisResponse)
async def get_nutrition_analysis(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    current_user: str = Depends(get_current_user)
):
    """
    获取营养素与食物来源分析

    Issue #25: 可视化报告：营养素与食物来源分析

    返回指定日期范围内的：
    - 宏量营养素比例（蛋白质、碳水、脂肪）
    - 各营养素摄入量vs推荐量
    - 食物类别分布
    """
    try:
        return await visualization_service.get_nutrition_analysis(
            user_email=current_user,
            start_date=start_date,
            end_date=end_date
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/time-series-trend", response_model=TimeSeriesTrendResponse)
async def get_time_series_trend(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    view_type: str = Query("day", description="视图类型: day, week, month"),
    current_user: str = Depends(get_current_user)
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
    try:
        return await visualization_service.get_time_series_trend(
            user_email=current_user,
            start_date=start_date,
            end_date=end_date,
            view_type=view_type
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/export-report", response_model=HealthReportExportResponse)
async def export_health_report(
    start_date: date = Query(..., description="开始日期(YYYY-MM-DD)"),
    end_date: date = Query(..., description="结束日期(YYYY-MM-DD)"),
    current_user: str = Depends(get_current_user)
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
    try:
        return await visualization_service.export_health_report(
            user_email=current_user,
            start_date=start_date,
            end_date=end_date
        )
    except ValueError as e:
        if "用户不存在" in str(e):
            raise HTTPException(status_code=404, detail=str(e))
        raise HTTPException(status_code=400, detail=str(e))
