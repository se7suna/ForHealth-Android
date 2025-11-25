from __future__ import annotations

from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, status

from app.routers.auth import get_current_user
from app.schemas.ai_assistant import (
    FoodImageRecognitionResponse,
    FoodRecognitionConfirmRequest,
    FoodRecognitionConfirmResponse,
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


