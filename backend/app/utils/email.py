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

        await aiosmtplib.send(
            message,
            hostname=settings.SMTP_HOST,
            port=settings.SMTP_PORT,
            username=settings.SMTP_USER,
            password=settings.SMTP_PASSWORD,
            start_tls=True,
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
