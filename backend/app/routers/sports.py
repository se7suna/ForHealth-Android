from fastapi import APIRouter, HTTPException, status, Depends
from app.routers.auth import get_current_user
from app.services import sports_service
from app.schemas.sports import (
    LogSportsRequest,
    CreateSportsRequest,
    UpdateSportsRequest,
    SearchSportsRequest,
    HistorySportsRequest,
    SearchSportsResponse,
    HistorySportsResponse,
    SimpleSportsResponse,
)

router = APIRouter(prefix="/sports", tags=["运动记录"])

# 记录运动
@router.post("/log-sports",response_model=SimpleSportsResponse)
async def log_sports(log_request: LogSportsRequest, current_user: str = Depends(get_current_user)):
    """
    记录运动及消耗卡路里
    - **sport_type**: 运动类型
    - **crearted_at**: 运动时间（默认当前时间）
    - **duration_time**: 运动持续时间（分钟，必须大于0）
    """
    result = await sports_service.log_sports(log_request,current_user)

    if result.inserted_id:
        return SimpleSportsResponse(success=True, message="运动记录已保存")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="保存运动记录失败"
        )

# 新建运动类型：自定义并写入表
@router.post("/create-sports",response_model=SimpleSportsResponse)
async def create_sports(create_request: CreateSportsRequest, current_user: str = Depends(get_current_user)):
    """
    创建自定义运动类型
    - **sport_type**: 运动类型
    - **METs**: 运动强度（必须大于0）
    """
    result=sports_service.create_sports(create_request,current_user)

    if result.inserted_id:
        return SimpleSportsResponse(success=True, message="自定义运动类型已创建")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="创建自定义运动类型失败"
        )

# 更新自定义运动类型
@router.post("/update-sports",response_model=SimpleSportsResponse)
async def update_sports(update_request: UpdateSportsRequest, current_user: str = Depends(get_current_user)):
    """
    更新自定义运动类型
    - **sport_type**: 运动类型
    - **METs**: 运动强度（必须大于0）
    """
    result = sports_service.update_sports(update_request,current_user)

    if result:
        return SimpleSportsResponse(success=True, message="自定义运动类型已更新")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="更新自定义运动类型失败"
        )

# 搜索运动类型
@router.post("/search-sports",response_model=list[SearchSportsResponse])
async def search_sports(search_request: SearchSportsRequest, current_user: str = Depends(get_current_user)):
    """
    搜索运动类型
    - **sport_type**: 运动类型（可选，模糊搜索）
    """
    return sports_service.search_sports(search_request,current_user)

# 搜索运动历史
@router.post("/history-sports",response_model=list[HistorySportsResponse])
async def history_sports(history_request: HistorySportsRequest, current_user: str = Depends(get_current_user)):
    """
    查询运动历史
    - **start_date**: 开始日期
    - **end_date**: 结束日期
    """
    return await sports_service.history_sports(history_request,current_user)  

# 获取全部运动记录，用于生成报告
@router.get("/sports-report",response_model=list[HistorySportsResponse])
async def sports_report(current_user: str = Depends(get_current_user)):
    history_request=HistorySportsRequest(
        start_date=None,
        end_date=None
    )
    # 需要添加一个函数生成报告内容
    return await sports_service.history_sports(history_request,current_user)