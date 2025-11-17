from datetime import datetime, timedelta
from typing import Optional, Dict
from app.utils.email import generate_verification_code, send_password_reset_email, send_registration_verification_email

# 内存存储验证码（生产环境应使用 Redis）
verification_codes: Dict[str, Dict] = {}
registration_codes: Dict[str, Dict] = {}  # 注册验证码存储


async def send_password_reset_code(email: str) -> bool:
    """
    发送密码重置验证码

    Args:
        email: 用户邮箱

    Returns:
        是否发送成功
    """
    # 生成验证码
    code = generate_verification_code()

    # 存储验证码和过期时间（5分钟）
    verification_codes[email] = {
        "code": code,
        "expires_at": datetime.utcnow() + timedelta(minutes=5),
    }

    # 发送邮件
    success = await send_password_reset_email(email, code)
    return success


def verify_code(email: str, code: str) -> bool:
    """
    验证验证码

    Args:
        email: 用户邮箱
        code: 验证码

    Returns:
        验证是否通过
    """
    if email not in verification_codes:
        return False

    stored_data = verification_codes[email]

    # 检查验证码是否正确
    if stored_data["code"] != code:
        return False

    # 检查是否过期
    if datetime.utcnow() > stored_data["expires_at"]:
        # 删除过期的验证码
        del verification_codes[email]
        return False

    return True


def invalidate_code(email: str):
    """使验证码失效"""
    if email in verification_codes:
        del verification_codes[email]


async def send_registration_code(email: str) -> bool:
    """
    发送注册验证码

    Args:
        email: 用户邮箱

    Returns:
        是否发送成功
    """
    # 生成验证码
    code = generate_verification_code()

    # 存储验证码和过期时间（5分钟）
    registration_codes[email] = {
        "code": code,
        "expires_at": datetime.utcnow() + timedelta(minutes=5),
    }

    # 发送邮件
    success = await send_registration_verification_email(email, code)
    return success


def verify_registration_code(email: str, code: str) -> bool:
    """
    验证注册验证码

    Args:
        email: 用户邮箱
        code: 验证码

    Returns:
        验证是否通过
    """
    if email not in registration_codes:
        return False

    stored_data = registration_codes[email]

    # 检查验证码是否正确
    if stored_data["code"] != code:
        return False

    # 检查是否过期
    if datetime.utcnow() > stored_data["expires_at"]:
        # 删除过期的验证码
        del registration_codes[email]
        return False

    return True


def invalidate_registration_code(email: str):
    """使注册验证码失效"""
    if email in registration_codes:
        del registration_codes[email]
