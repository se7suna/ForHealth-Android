"""
外部API配置文件

配置薄荷健康(Boohee) API的相关信息
"""

# ============================================================
# 薄荷健康 API (Boohee) 配置
# ============================================================

# API基础URL
BOOHEE_API_URL = "https://fc.boohee.com"

# 应用ID和应用密钥列表（从薄荷健康平台获取）
# 由于每天单个账号有限制，所以需要使用多个应用ID和应用密钥
# 建议从https://fc.boohee.com中注册，只需要填写手机号和验证码即可
BOOHEE_ACCOUNTS = [
    {"app_id": "2c98025428", "app_key": "eaa6a8bcdf6a760b8624cdd5f1d988d4"},
    {"app_id": "9438bda1f3", "app_key": "57f1f2639c667e6ad572c82fd44fafc4"},
    {"app_id": "69cbee103b", "app_key": "122e5732e99a722611abaa5573ba5edc"},
    {"app_id": "5c44cee028", "app_key": "3d5c25cfe7b2a33ae104d0f36ecb07a1"},
]

# 当前使用的账号索引（从0开始）
# 当检测到账号达到限制时，会自动切换到下一个账号
# 如果到了最后一个账号，会循环回第一个账号
BOOHEE_CURRENT_ACCOUNT_INDEX = 0

# 为了向后兼容，保留原有的变量名（使用第一个账号）
BOOHEE_APP_ID = BOOHEE_ACCOUNTS[0]["app_id"]
BOOHEE_APP_KEY = BOOHEE_ACCOUNTS[0]["app_key"]

# AccessToken缓存时间（秒），默认1小时
# 注意：实际Token有效期约为1个月，这里设置缓存时间用于本地缓存管理
BOOHEE_TOKEN_CACHE_TIME = 3600

# API请求超时时间（秒）
EXTERNAL_API_TIMEOUT = 10

# 是否启用外部API（用于测试时关闭）
EXTERNAL_API_ENABLED = True


# ============================================================
# 百炼 / 通义千问 多模态 API 配置
# ============================================================

# 建议不要把真实密钥提交到仓库，以下仅为占位符：
# 请在本地开发环境中把 QWEN_API_KEY 修改为你的真实密钥，
# 或者通过环境变量 DASHSCOPE_API_KEY / QWEN_API_KEY 来配置。
QWEN_API_KEY = "sk-83b57c8f1845479599839bca6aee0431"