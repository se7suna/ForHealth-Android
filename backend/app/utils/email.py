import aiosmtplib
import random
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.config import settings


async def send_email(to_email: str, subject: str, body: str) -> bool:
    """发送邮件"""
    try:
        message = MIMEMultipart()
        message["From"] = f"{settings.SMTP_FROM_NAME} <{settings.SMTP_FROM_EMAIL}>"
        message["To"] = to_email
        message["Subject"] = subject

        message.attach(MIMEText(body, "html"))

        # Postfix SMTP 发送（端口25无需认证和TLS）
        await aiosmtplib.send(
            message,
            hostname=settings.SMTP_HOST,
            port=settings.SMTP_PORT,
            username=settings.SMTP_USER or None,
            password=settings.SMTP_PASSWORD or None,
            start_tls=False,  # Postfix 内网通信无需 TLS
        )
        return True
    except Exception as e:
        print(f"❌ 邮件发送失败: {e}")
        return False


def generate_verification_code() -> str:
    """生成6位数字验证码"""
    return str(random.randint(100000, 999999))


async def send_password_reset_email(to_email: str, verification_code: str) -> bool:
    """发送密码重置邮件"""
    subject = "密码重置验证码 - For Health"
    body = f"""
    <html>
        <body>
            <h2>密码重置验证码</h2>
            <p>您好！</p>
            <p>您正在重置 For Health 账户密码。您的验证码是：</p>
            <h1 style="color: #4CAF50; font-size: 32px; letter-spacing: 5px;">{verification_code}</h1>
            <p>此验证码将在 <strong>5分钟</strong> 后失效。</p>
            <p>如果这不是您的操作，请忽略此邮件。</p>
            <hr>
            <p style="color: #666; font-size: 12px;">此邮件由系统自动发送，请勿回复。</p>
        </body>
    </html>
    """
    return await send_email(to_email, subject, body)


async def send_registration_verification_email(to_email: str, verification_code: str) -> bool:
    """发送注册验证邮件"""
    subject = "邮箱验证码 - For Health"
    body = f"""
    <html>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px 10px 0 0;">
                <h1 style="color: white; margin: 0; text-align: center;">欢迎使用 For Health</h1>
            </div>
            <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;">
                <h2 style="color: #333;">邮箱验证</h2>
                <p style="color: #666; font-size: 16px;">您好！</p>
                <p style="color: #666; font-size: 16px;">感谢您注册 For Health 健康管理系统。请使用以下验证码完成注册：</p>
                <div style="background: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
                    <h1 style="color: #4CAF50; font-size: 36px; letter-spacing: 8px; margin: 0;">{verification_code}</h1>
                </div>
                <p style="color: #666; font-size: 14px;">此验证码将在 <strong style="color: #4CAF50;">5分钟</strong> 后失效。</p>
                <p style="color: #666; font-size: 14px;">如果这不是您的操作，请忽略此邮件。</p>
                <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                <p style="color: #999; font-size: 12px; text-align: center;">此邮件由 For Health 系统自动发送，请勿回复。</p>
            </div>
        </body>
    </html>
    """
    return await send_email(to_email, subject, body)
