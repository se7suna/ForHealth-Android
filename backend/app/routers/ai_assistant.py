from __future__ import annotations

from typing import Optional
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, status, Form
from datetime import datetime

from app.routers.auth import get_current_user
from app.schemas.ai_assistant import (
    FoodRecognitionConfirmResponse,
    QuestionRequest,
    QuestionResponse,
    DietAnalysisRequest,
    DietAnalysisResponse,
    MealRecommendationResponse,
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
    "/ask",
    response_model=QuestionResponse,
    summary="健康知识问答",
    description="统一的健康知识问答接口，支持营养、运动、健康等相关问题。AI会自动识别问题类型并给出专业回答。",
)
async def ask_question(
    payload: QuestionRequest,
    current_user: str = Depends(get_current_user),
) -> QuestionResponse:
    """
    回答用户关于健康知识的问题（营养、运动、健康等）。
    
    AI 会自动识别问题类型（营养/运动/综合健康），并基于用户档案信息给出个性化回答。
    
    **上下文信息（context）**：
    - `context` 参数为可选项，如果未提供，系统会自动从用户档案中读取相关信息（如体重、活动水平、健康目标等）
    - 如果提供了 `context`，则优先使用请求中的值，用于临时覆盖用户档案中的信息
    - 支持的 context 字段：`user_goal`（用户目标）、`activity_level`（活动水平）、`weight`（体重）、`height`（身高）、`age`（年龄）
    
    **示例**：
    ```python
    POST /api/ai/ask
    {
        "question": "蛋白质补充的最佳时间是什么时候？",
        "context": {"user_goal": "增肌"}
    }
    
    POST /api/ai/ask
    {
        "question": "如何制定一个有效的减脂运动计划？"
    }
    ```
    """
    try:
        return await ai_assistant_service.answer_question(current_user, payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"知识问答失败：{str(e)}",
        )


@router.post(
    "/diet/analyze",
    response_model=DietAnalysisResponse,
    summary="饮食分析与建议",
    description="分析用户近期饮食记录，关注卡路里平衡和营养素平衡，找出最显著的问题并给出一句话建议。",
)
async def analyze_diet(
    payload: DietAnalysisRequest = None,
    current_user: str = Depends(get_current_user),
) -> DietAnalysisResponse:
    """
    分析用户近期饮食记录，生成一句话建议。
    
    **分析维度**：
    1. 消耗和摄入（卡路里）的不平衡
    2. 三种营养素是否平衡：蛋白质、碳水化合物、脂肪
    
    **返回**：
    - 一句话建议（亲和语气），指出最显著的问题或给予鼓励
    - 详细分析数据（可选展示）
    
    **示例**：
    ```python
    POST /api/ai/diet/analyze
    {
        "days": 7
    }
    ```
    
    **响应示例**：
    ```json
    {
        "success": true,
        "message": "最近蛋白质摄入偏低哦～建议多吃些鸡蛋、鸡胸肉补充一下！💪",
        "analysis": {
            "days_analyzed": 7,
            "avg_calories_intake": 1650,
            "calorie_balance": "略有不足",
            "macro_ratio": {"protein_percent": 15, "carbs_percent": 55, "fat_percent": 30}
        }
    }
    ```
    """
    days = payload.days if payload else 7
    
    try:
        return await ai_assistant_service.analyze_recent_diet(current_user, days)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"饮食分析失败：{str(e)}",
        )


@router.get(
    "/meal/recommend",
    response_model=MealRecommendationResponse,
    summary="智能菜式推荐",
    description="根据当前时间和用户近期营养摄入情况，推荐具体菜式。",
)
async def recommend_meal(
    current_user: str = Depends(get_current_user),
) -> MealRecommendationResponse:
    """
    智能推荐菜式。
    
    **推荐依据**：
    1. 当前时间（早餐/午餐/晚餐/加餐时间）
    2. 用户近期营养摄入情况（缺什么补什么）
    
    **返回**：
    - 推荐语（包含时间提醒和菜式推荐）
    - 推荐的菜式名称
    - 推荐理由
    
    **响应示例**：
    ```json
    {
        "success": true,
        "message": "到中午了！记得吃午饭哦！向你推荐鸡排饭，可以补充蛋白质～🍗",
        "meal_type": "午餐",
        "recommended_dish": "鸡排饭",
        "reason": "最近蛋白质摄入偏低，鸡排富含优质蛋白",
        "nutrition_highlight": "高蛋白、适量碳水"
    }
    ```
    """
    try:
        return await ai_assistant_service.recommend_meal(current_user)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"菜式推荐失败：{str(e)}",
        )
