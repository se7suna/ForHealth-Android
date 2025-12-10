from fastapi import APIRouter, HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.schemas.user import (
    UserRegisterRequest,
    UserLoginRequest,
    TokenResponse,
    MessageResponse,
    PasswordResetRequest,
    PasswordResetVerify,
    SendRegistrationCodeRequest,
    RefreshTokenRequest,
)
from app.services import user_service, auth_service
from app.utils.security import (
    get_password_hash,
    verify_password,
    create_access_token,
    decode_access_token,
    create_refresh_token,
    decode_refresh_token,
)

router = APIRouter(prefix="/auth", tags=["认证"])
security = HTTPBearer()


@router.post("/send-verification-code", response_model=MessageResponse)
async def send_registration_verification_code(request: SendRegistrationCodeRequest):
    """
    发送注册验证码

    - **email**: 邮箱地址

    系统会向邮箱发送6位数字验证码，有效期5分钟
    """
    # 检查邮箱是否已注册
    existing_user = await user_service.get_user_by_email(request.email)
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail="该邮箱已被注册"
        )

    # 发送验证码
    success = await auth_service.send_registration_code(request.email)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="验证码发送失败，请稍后重试",
        )

    return MessageResponse(message="验证码已发送至邮箱，有效期5分钟")


@router.post("/register", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserRegisterRequest):
    """
    用户注册

    - **email**: 邮箱地址（必须是有效的邮箱格式）
    - **username**: 用户名（2-50个字符）
    - **password**: 密码（至少6个字符）
    - **verification_code**: 邮箱验证码（6位数字）
    """
    # 验证邮箱验证码
    if not auth_service.verify_registration_code(user_data.email, user_data.verification_code):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="验证码错误或已过期",
        )

    # 检查邮箱是否已注册
    existing_user = await user_service.get_user_by_email(user_data.email)
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail="该邮箱已被注册"
        )

    # 密码加密
    hashed_password = get_password_hash(user_data.password)

    # 创建用户
    user = await user_service.create_user(
        user_data.email, user_data.username, hashed_password
    )

    # 使验证码失效
    auth_service.invalidate_registration_code(user_data.email)

    return MessageResponse(
        message="注册成功，请继续填写身体基本数据",
        data={
            "email": user["email"],
            "username": user["username"],
        },
    )


@router.post("/login", response_model=TokenResponse)
async def login(login_data: UserLoginRequest):
    """
    用户登录

    - **email**: 注册时使用的邮箱
    - **password**: 密码

    返回 JWT access token 和 refresh token
    """
    # 获取用户
    user = await user_service.get_user_by_email(login_data.email)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="邮箱或密码错误"
        )

    # 验证密码
    if not verify_password(login_data.password, user["hashed_password"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="邮箱或密码错误"
        )

    # 生成 access token (短期有效)
    access_token = create_access_token(data={"sub": user["email"]})
    # 生成 refresh token (长期有效)
    refresh_token = create_refresh_token(data={"sub": user["email"]})

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token
    )


@router.post("/password-reset/send-code", response_model=MessageResponse)
async def send_password_reset_code(reset_request: PasswordResetRequest):
    """
    发送密码重置验证码

    - **email**: 注册时使用的邮箱

    系统会向邮箱发送6位数字验证码，有效期5分钟
    """
    # 检查邮箱是否存在
    user = await user_service.get_user_by_email(reset_request.email)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="该邮箱未注册"
        )

    # 发送验证码
    success = await auth_service.send_password_reset_code(reset_request.email)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="验证码发送失败，请稍后重试",
        )

    return MessageResponse(message="验证码已发送至邮箱，有效期5分钟")


@router.post("/password-reset/verify", response_model=MessageResponse)
async def reset_password(reset_data: PasswordResetVerify):
    """
    验证验证码并重置密码

    - **email**: 邮箱
    - **verification_code**: 6位数字验证码
    - **new_password**: 新密码（至少6个字符）
    - **confirm_password**: 确认新密码（必须与新密码一致）
    """
    # 验证验证码
    if not auth_service.verify_code(reset_data.email, reset_data.verification_code):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="验证码错误或已过期",
        )

    # 密码加密
    hashed_password = get_password_hash(reset_data.new_password)

    # 更新密码
    success = await user_service.update_password(reset_data.email, hashed_password)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="密码更新失败",
        )

    # 使验证码失效
    auth_service.invalidate_code(reset_data.email)

    return MessageResponse(message="密码重置成功，请使用新密码登录")


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(refresh_request: RefreshTokenRequest):
    """
    刷新 access token

    - **refresh_token**: 长期有效的 refresh token

    返回新的 access token 和 refresh token
    """
    # 验证 refresh token
    payload = decode_refresh_token(refresh_request.refresh_token)

    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的 refresh token",
        )

    email = payload.get("sub")
    if email is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的 refresh token",
        )

    # 验证用户是否存在
    user = await user_service.get_user_by_email(email)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="用户不存在",
        )

    # 生成新的 tokens
    new_access_token = create_access_token(data={"sub": email})
    new_refresh_token = create_refresh_token(data={"sub": email})

    return TokenResponse(
        access_token=new_access_token,
        refresh_token=new_refresh_token
    )


# 依赖项：获取当前登录用户
async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> str:
    """从 JWT token 中获取当前用户邮箱"""
    token = credentials.credentials
    payload = decode_access_token(token)

    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的认证凭证",
            headers={"WWW-Authenticate": "Bearer"},
        )

    email = payload.get("sub")
    if email is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="无效的认证凭证"
        )

    return email
