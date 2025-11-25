from __future__ import annotations

from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, status, Query
from datetime import date

from app.routers.auth import get_current_user
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    FoodRecognitionConfirmRequest,
    FoodRecognitionConfirmResponse,
    MealPlanRequest,
    MealPlanResponse,
    NutritionQuestionRequest,
    NutritionQuestionResponse,
    SportsQuestionRequest,
    SportsQuestionResponse,
    ReminderSettingsRequest,
    ReminderSettingsResponse,
    NotificationListResponse,
    NotificationReadRequest,
    DailyFeedbackResponse,
)
from app.services import ai_assistant_service


router = APIRouter(prefix="/ai", tags=["AI 助手"])


@router.post(
    "/food/recognize-image",
    response_model=FoodImageRecognitionResponse,
    summary="拍照识别食物",
    description="上传一张食物照片，调用多模态大模型进行识别。若本地数据库中存在匹配食物，则优先使用数据库的营养信息。",
)
async def recognize_food_from_image(
    file: UploadFile = File(..., description="食物图片文件"),
    current_user: str = Depends(get_current_user),
) -> FoodImageRecognitionResponse:
    """
    拍照识别食物：

    - 输入：前端通过 multipart/form-data 上传图片文件字段 `file`；
    - 输出：识别到的食物列表及汇总营养信息。
    """
    try:
        return await ai_assistant_service.recognize_food_image(file, current_user)
    except HTTPException:
        # 透传业务异常
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"食物图片识别失败：{str(e)}",
        )


@router.post(
    "/food/confirm-recognition",
    response_model=FoodRecognitionConfirmResponse,
    summary="确认识别结果并写入饮食日志",
)
async def confirm_food_recognition(
    payload: FoodRecognitionConfirmRequest,
    current_user: str = Depends(get_current_user),
) -> FoodRecognitionConfirmResponse:
    """
    前端在用户确认 / 编辑识别结果后，调用该接口将食物记录写入饮食日志。
    """
    try:
        return await ai_assistant_service.confirm_food_recognition(current_user, payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"确认识别结果失败：{str(e)}",
        )


@router.post(
    "/meal-plan/generate",
    response_model=MealPlanResponse,
    summary="生成个性化饮食计划",
)
async def generate_meal_plan(
    payload: MealPlanRequest,
    current_user: str = Depends(get_current_user),
) -> MealPlanResponse:
    """
    根据用户的个人信息和偏好，调用大模型生成每日饮食计划。
    """
    try:
        return await ai_assistant_service.generate_meal_plan(current_user, payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"生成饮食计划失败：{str(e)}",
        )


@router.post(
    "/nutrition/ask",
    response_model=NutritionQuestionResponse,
    summary="营养知识问答",
)
async def ask_nutrition_question(
    payload: NutritionQuestionRequest,
    current_user: str = Depends(get_current_user),
) -> NutritionQuestionResponse:
    """
    回答用户关于营养知识的问题。
    
    使用专业 prompt 引导大模型参考营养学知识回答，并自动过滤敏感信息。
    
    **上下文信息（context）**：
    - `context` 参数为可选项，如果未提供，系统会自动从用户档案中读取相关信息（如体重、活动水平、健康目标等）
    - 如果提供了 `context`，则优先使用请求中的值，用于临时覆盖用户档案中的信息
    - 支持的 context 字段：`user_goal`（用户目标）、`activity_level`（活动水平）、`weight`（体重）、`height`（身高）、`age`（年龄）
    """
    try:
        return await ai_assistant_service.answer_nutrition_question(current_user, payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"营养知识问答失败：{str(e)}",
        )


@router.post(
    "/sports/ask",
    response_model=SportsQuestionResponse,
    summary="运动知识问答",
)
async def ask_sports_question(
    payload: SportsQuestionRequest,
    current_user: str = Depends(get_current_user),
) -> SportsQuestionResponse:
    """
    回答用户关于运动知识的问题。
    
    使用专业 prompt 引导大模型参考运动科学知识回答，并自动过滤敏感信息。
    
    **上下文信息（context）**：
    - `context` 参数为可选项，如果未提供，系统会自动从用户档案中读取相关信息（如体重、身高、活动水平、健康目标等）
    - 如果提供了 `context`，则优先使用请求中的值，用于临时覆盖用户档案中的信息
    - 支持的 context 字段：`user_goal`（用户目标）、`activity_level`（活动水平）、`weight`（体重）、`height`（身高）、`age`（年龄）
    """
    try:
        return await ai_assistant_service.answer_sports_question(current_user, payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"运动知识问答失败：{str(e)}",
        )


# ========== 智能提醒与反馈 ==========

@router.get(
    "/reminders/settings",
    response_model=ReminderSettingsResponse,
    summary="获取提醒设置",
)
async def get_reminder_settings(
    current_user: str = Depends(get_current_user),
) -> ReminderSettingsResponse:
    """
    获取当前用户的提醒设置。
    
    如果用户尚未设置，将返回默认设置。
    """
    try:
        from app.schemas.ai_assistant import ReminderSettings
        
        settings_dict = await ai_assistant_service.get_reminder_settings(current_user)
        settings = ReminderSettings(**settings_dict)
        
        return ReminderSettingsResponse(
            success=True,
            message="获取提醒设置成功",
            settings=settings
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取提醒设置失败：{str(e)}",
        )


@router.put(
    "/reminders/settings",
    response_model=ReminderSettingsResponse,
    summary="更新提醒设置",
)
async def update_reminder_settings(
    payload: ReminderSettingsRequest,
    current_user: str = Depends(get_current_user),
) -> ReminderSettingsResponse:
    """
    更新当前用户的提醒设置。
    
    支持的设置项：
    - `meal_reminders`: 是否开启餐次提醒
    - `meal_reminder_times`: 餐次提醒时间列表（格式：HH:MM）
    - `record_reminders`: 是否开启记录提醒
    - `record_reminder_hours`: 未记录提醒间隔（小时）
    - `goal_reminders`: 是否开启目标达成提醒
    - `motivational_messages`: 是否开启鼓励性消息
    """
    try:
        settings_dict = payload.settings.dict()
        updated_settings = await ai_assistant_service.update_reminder_settings(
            current_user, settings_dict
        )
        
        from app.schemas.ai_assistant import ReminderSettings
        settings = ReminderSettings(**updated_settings)
        
        return ReminderSettingsResponse(
            success=True,
            message="提醒设置已更新",
            settings=settings
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"更新提醒设置失败：{str(e)}",
        )


@router.get(
    "/notifications",
    response_model=NotificationListResponse,
    summary="获取通知列表",
)
async def get_notifications(
    limit: int = Query(default=50, ge=1, le=100, description="返回数量限制"),
    offset: int = Query(default=0, ge=0, description="偏移量（用于分页）"),
    unread_only: bool = Query(default=False, description="是否只返回未读通知"),
    current_user: str = Depends(get_current_user),
) -> NotificationListResponse:
    """
    获取当前用户的通知列表。
    
    - **limit**: 返回数量限制（1-100，默认50）
    - **offset**: 偏移量，用于分页（默认0）
    - **unread_only**: 是否只返回未读通知（默认false）
    
    返回结果按创建时间倒序排列（最新的在前）。
    """
    try:
        from app.schemas.ai_assistant import NotificationMessageResponse
        
        notifications, total, unread_count = await ai_assistant_service.get_notifications(
            current_user, limit, offset, unread_only
        )
        
        notification_responses = [
            NotificationMessageResponse(
                id=n["id"],
                type=n["type"],
                title=n["title"],
                content=n["content"],
                created_at=n["created_at"],
                read=n.get("read", False),
                action_url=n.get("action_url"),
                priority=n.get("priority", "normal")
            )
            for n in notifications
        ]
        
        return NotificationListResponse(
            total=total,
            unread_count=unread_count,
            notifications=notification_responses
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取通知列表失败：{str(e)}",
        )


@router.post(
    "/notifications/mark-read",
    summary="标记通知为已读",
)
async def mark_notifications_read(
    payload: NotificationReadRequest,
    current_user: str = Depends(get_current_user),
):
    """
    标记指定的通知为已读。
    
    可以一次标记多个通知。
    """
    try:
        updated_count = await ai_assistant_service.mark_notifications_read(
            current_user, payload.notification_ids
        )
        
        return {
            "success": True,
            "message": f"已标记 {updated_count} 条通知为已读",
            "updated_count": updated_count
        }
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"标记通知为已读失败：{str(e)}",
        )


@router.get(
    "/feedback/daily/{target_date}",
    response_model=DailyFeedbackResponse,
    summary="获取每日反馈",
)
async def get_daily_feedback(
    target_date: date,
    current_user: str = Depends(get_current_user),
) -> DailyFeedbackResponse:
    """
    获取指定日期的每日反馈数据。
    
    - **target_date**: 目标日期（格式：YYYY-MM-DD）
    
    返回内容包括：
    - 当日营养摄入汇总
    - 目标完成进度
    - 目标状态（正常/超标/不足）
    - 个性化建议（由AI生成）
    - 相关通知消息（如果有）
    """
    try:
        from app.schemas.ai_assistant import FeedbackDataResponse, NotificationMessageResponse
        
        result = await ai_assistant_service.get_daily_feedback(current_user, target_date)
        
        feedback_data = result["feedback"]
        notification_data = result.get("notification")
        
        feedback = FeedbackDataResponse(**feedback_data)
        
        notification = None
        if notification_data:
            notification = NotificationMessageResponse(
                id=notification_data["id"],
                type=notification_data["type"],
                title=notification_data["title"],
                content=notification_data["content"],
                created_at=notification_data["created_at"],
                read=notification_data.get("read", False),
                action_url=notification_data.get("action_url"),
                priority=notification_data.get("priority", "normal")
            )
        
        return DailyFeedbackResponse(
            success=True,
            feedback=feedback,
            notification=notification
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取每日反馈失败：{str(e)}",
        )


