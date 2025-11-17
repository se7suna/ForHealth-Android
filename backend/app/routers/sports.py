from fastapi import APIRouter, HTTPException, status, Depends
from app.routers.auth import get_current_user
from app.services import sports_service
from app.schemas.sports import (
    LogSportsRequest,
    UpdateSportsRecordRequest,
    CreateSportsRequest,
    UpdateSportsRequest,
    SearchSportRecordsRequest,
    SearchSportsResponse,
    SearchSportRecordsResponse,
    SimpleSportsResponse,
)

router = APIRouter(prefix="/sports", tags=["运动记录"])


# 新建运动类型：自定义并写入表
@router.post("/create-sport",response_model=SimpleSportsResponse)
async def create_sports(create_request: CreateSportsRequest, current_user: str = Depends(get_current_user)):
    """
    创建自定义运动类型
    - **sport_type**: 运动类型
    - **describe**: 运动描述
    - **METs**: 运动强度（必须大于0）
    """
    result=await sports_service.create_sports(create_request,current_user)

    if result.inserted_id:
        return SimpleSportsResponse(success=True, message="自定义运动类型已创建")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="创建自定义运动类型失败"
        )

# 更新自定义运动类型
@router.post("/update-sport",response_model=SimpleSportsResponse)
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

# 删除自定义运动类型
@router.get("/delete-sport/{sport_type}",response_model=SimpleSportsResponse)
async def delete_sports(sport_type: str, current_user: str = Depends(get_current_user)):
    """
    删除自定义运动类型
    - **sport_type**: 运动类型
    """
    result = await sports_service.delete_sports(sport_type, current_user)

    if result:
        return SimpleSportsResponse(success=True, message="自定义运动类型已删除")
    else:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="删除自定义运动类型失败，运动类型不存在或无权删除"
        )

# 获取用户可用运动类型列表
@router.get("/get-available-sports-types", response_model=list[SearchSportsResponse])
async def get_available_sports_types(current_user: str = Depends(get_current_user)):
    """
    获取用户可用的运动类型列表
    包括系统默认运动类型和用户自定义的运动类型
    """
    return await sports_service.get_available_sports_types(current_user)

# 记录运动记录
@router.post("/log-sports",response_model=SimpleSportsResponse)
async def log_sports_record(log_request: LogSportsRequest, current_user: str = Depends(get_current_user)):
    """
    记录运动及消耗卡路里
    - **sport_type**: 运动类型
    - **created_at**: 开始运动时间（默认当前时间）
    - **duration_time**: 运动持续时间（分钟，必须大于0）
    """
    result = await sports_service.log_sports_record(log_request,current_user)

    if result["record_id"]:
        return SimpleSportsResponse(success=True, message="运动记录已保存")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="保存运动记录失败"   
        )
    
# 更新运动记录
@router.post("/update-sport-record",response_model=SimpleSportsResponse)
async def update_sports_record(update_request: UpdateSportsRecordRequest, current_user: str = Depends(get_current_user)):
    """
    更新运动记录
    - **record_id**: 运动记录ID
    - **sport_type**: 运动类型
    - **created_at**: 开始运动时间（默认当前时间）
    - **duration_time**: 运动持续时间（分钟，必须大于0）
    """
    result = await sports_service.update_sports_record(update_request,current_user)

    if result:
        return SimpleSportsResponse(success=True, message="运动记录已更新")
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="更新运动记录失败"
        )

# 删除运动记录
@router.get("/delete-sport-record/{record_id}",response_model=SimpleSportsResponse)
async def delete_sports_record(record_id: str, current_user: str = Depends(get_current_user)):
    """
    删除运动记录
    - **record_id**: 运动记录ID
    """
    result = await sports_service.delete_sports_record(record_id, current_user)

    if result:
        return SimpleSportsResponse(success=True, message="运动记录已删除")
    else:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="删除运动记录失败，记录不存在或无权删除"
        )

# 搜索运动记录
@router.post("/search-sports-records",response_model=list[SearchSportRecordsResponse])
async def search_sports_records(search_request: SearchSportRecordsRequest, current_user: str = Depends(get_current_user)):
    """
    查询运动历史（查询条件均可选）
    - **start_date**: 开始日期
    - **end_date**: 结束日期
    - **sport_type**: 运动类型
    """
    return await sports_service.search_sports_record(search_request,current_user)  


# 获取全部运动记录
@router.get("/get-all-sports-records",response_model=list[SearchSportRecordsResponse])
async def get_all_sports_sports(current_user: str = Depends(get_current_user)):
    """
    获取用户全部运动记录
    """
    search_request = type('obj', (object,), {})()
    search_request.start_date = None
    search_request.end_date = None
    search_request.sport_type = None
    return await sports_service.search_sports_record(search_request,current_user)



# 获取运动报告
@router.get("/sports-report")
async def sports_report(current_user: str = Depends(get_current_user)):
    """
    获取用户运动报告，包括运动统计数据和详细分析
    """
    return await sports_service.generate_sports_report(current_user)