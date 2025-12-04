from __future__ import annotations

from typing import Optional
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, status, Query, Form
from datetime import date, datetime

from app.routers.auth import get_current_user
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    FoodRecognitionConfirmRequest,
    FoodRecognitionConfirmResponse,
    MealPlanRequest,
    MealPlanResponse,
    QuestionRequest,
    QuestionResponse,
    ReminderSettingsRequest,
    ReminderSettingsResponse,
    NotificationListResponse,
    NotificationReadRequest,
    DailyFeedbackResponse,
)
from app.services import ai_assistant_service


router = APIRouter(prefix="/ai", tags=["AI 助手"])


@router.post(
    "/food/recognize",
    response_model=FoodRecognitionConfirmResponse,
    summary="拍照识别食物并自动处理",
    description="上传一张食物照片，调用多模态大模型进行识别，并自动处理识别结果（创建/匹配食物）。若本地数据库中存在匹配食物，则优先使用数据库的营养信息。",
)
async def recognize_and_process_food(
    file: UploadFile = File(..., description="食物图片文件"),
    meal_type: Optional[str] = Form(None, description="餐次类型（可选：早餐、午餐、晚餐、加餐）"),
    notes: Optional[str] = Form(None, description="备注（可选）"),
    recorded_at: Optional[str] = Form(None, description="摄入时间（可选，ISO格式，如：2025-11-03T12:30:00）"),
    current_user: str = Depends(get_current_user),
) -> FoodRecognitionConfirmResponse:
    """
    拍照识别食物并自动处理识别结果（合并了识别和确认功能）。
    
    **功能**：
    1. 上传图片并调用AI识别食物
    2. 自动处理识别结果（创建/匹配本地食物）
    3. 返回处理后的食物信息（包含 food_id 和 serving_amount 建议）
    4. 识别完成后自动删除临时图片
    
    **输入**（multipart/form-data）：
    - **file**: 食物图片文件（必填）
    - **meal_type**: 餐次类型（可选：早餐、午餐、晚餐、加餐）
    - **notes**: 备注（可选）
    - **recorded_at**: 摄入时间（可选，ISO格式）
    
    **输出**：
    - **processed_foods**: 处理后的食物信息列表（包含 food_id 和 serving_amount）
    - **total_foods**: 成功处理的食物数量
    
    **重要**：此接口不创建饮食记录。前端需要：
    1. 调用此接口获取处理后的食物信息（包含 food_id）
    2. 然后调用 `POST /api/food/record` 创建饮食记录
    
    **示例流程**：
    ```python
    # 1. 上传图片并识别处理
    with open("food.jpg", "rb") as f:
        response = await client.post(
            "/api/ai/food/recognize",
            files={"file": f},
            data={
                "meal_type": "午餐",
                "notes": "AI识别",
                "recorded_at": "2025-11-03T12:30:00"
            }
        )
    processed_foods = response.json()["processed_foods"]
    
    # 2. 对每个处理后的食物，调用创建记录接口
    for food in processed_foods:
        record_payload = {
            "food_id": food["food_id"],
            "serving_amount": food["serving_amount"],
            "recorded_at": "2025-11-03T12:30:00",
            "meal_type": "午餐",
            "notes": "AI识别",
            "source": "local"
        }
        await client.post("/api/food/record", json=record_payload)
    ```
    """
    # 解析 recorded_at（如果提供）
    parsed_recorded_at = None
    if recorded_at:
        try:
            parsed_recorded_at = datetime.fromisoformat(recorded_at.replace("Z", "+00:00"))
        except Exception:
            # 如果解析失败，忽略该参数
            pass
    
    # 验证 meal_type（如果提供）
    if meal_type and meal_type not in ["早餐", "午餐", "晚餐", "加餐", "breakfast", "lunch", "dinner", "snack"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="餐次类型必须是：早餐、午餐、晚餐、加餐 之一",
        )
    
    try:
        return await ai_assistant_service.recognize_and_process_food_image(
            file=file,
            user_email=current_user,
            meal_type=meal_type,
            notes=notes,
            recorded_at=parsed_recorded_at,
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"食物图片识别处理失败：{str(e)}",
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
    "/ask/{question_type}",
    response_model=QuestionResponse,
    summary="知识问答（统一接口）",
    description="统一的问答接口，支持营养和运动知识问答。",
)
async def ask_question(
    question_type: str,
    payload: QuestionRequest,
    current_user: str = Depends(get_current_user),
) -> QuestionResponse:
    """
    回答用户关于健康知识的问题（支持营养和运动）。
    
    **问题类型（question_type）**：
    - `nutrition`：营养知识问答
    - `sports`：运动知识问答
    
    **上下文信息（context）**：
    - `context` 参数为可选项，如果未提供，系统会自动从用户档案中读取相关信息（如体重、活动水平、健康目标等）
    - 如果提供了 `context`，则优先使用请求中的值，用于临时覆盖用户档案中的信息
    - 支持的 context 字段：`user_goal`（用户目标）、`activity_level`（活动水平）、`weight`（体重）、`height`（身高）、`age`（年龄）
    
    **示例**：
    ```python
    # 营养知识问答
    POST /api/ai/ask/nutrition
    {
        "question": "蛋白质补充的最佳时间是什么时候？",
        "context": {"user_goal": "增肌"}
    }
    
    # 运动知识问答
    POST /api/ai/ask/sports
    {
        "question": "如何制定一个有效的减脂运动计划？",
        "context": {"activity_level": "moderately_active"}
    }
    ```
    """
    if question_type not in ["nutrition", "sports"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"不支持的问题类型: {question_type}，支持的类型：nutrition（营养）、sports（运动）",
        )
    
    try:
        return await ai_assistant_service.answer_question(current_user, payload, question_type)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"知识问答失败：{str(e)}",
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


