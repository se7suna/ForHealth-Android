"""
测试 JWT Token 过期时间
"""
import sys
from pathlib import Path
backend_path = str(Path(__file__).parent.absolute())
sys.path.insert(0, backend_path)

from datetime import datetime, timedelta
from app.utils.security import create_access_token, decode_access_token
from app.config import settings

# 创建 token
print(f"配置的过期时间: {settings.ACCESS_TOKEN_EXPIRE_MINUTES} 分钟")
print(f"当前 UTC 时间: {datetime.utcnow()}")

token = create_access_token(data={"sub": "test@example.com"})
print(f"\n生成的 Token: {token[:50]}...")

# 解码 token
payload = decode_access_token(token)
if payload:
    exp_timestamp = payload.get("exp")
    if exp_timestamp:
        exp_datetime = datetime.fromtimestamp(exp_timestamp)
        current_time = datetime.utcnow()

        print(f"\nToken 过期时间戳: {exp_timestamp}")
        print(f"Token 过期时间: {exp_datetime}")
        print(f"当前 UTC 时间: {current_time}")

        # 计算实际过期分钟数
        time_diff = exp_datetime - current_time
        minutes_until_expiry = time_diff.total_seconds() / 60

        print(f"\n实际剩余时间: {minutes_until_expiry:.2f} 分钟")
        print(f"预期剩余时间: {settings.ACCESS_TOKEN_EXPIRE_MINUTES} 分钟")

        if abs(minutes_until_expiry - settings.ACCESS_TOKEN_EXPIRE_MINUTES) > 1:
            print("\n⚠️  警告：实际过期时间与配置不符！")
        else:
            print("\n✅ Token 过期时间设置正确")
else:
    print("\n❌ Token 解码失败")
