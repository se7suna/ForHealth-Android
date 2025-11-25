"""
外部API配置文件

配置薄荷健康(Boohee) API的相关信息
"""

# ============================================================
# 薄荷健康 API (Boohee) 配置
# ============================================================

# API基础URL
BOOHEE_API_URL = "https://fc.boohee.com"

# 应用ID（从薄荷健康平台获取）
# 应用密钥（从薄荷健康平台获取）
#BOOHEE_APP_ID = "69cbee103b"
#BOOHEE_APP_KEY = "122e5732e99a722611abaa5573ba5edc"

# 由于每天单个账号有限制，所以需要使用多个应用ID和应用密钥
# 备用id和key
#BOOHEE_APP_ID = "5c44cee028"
#BOOHEE_APP_KEY = "3d5c25cfe7b2a33ae104d0f36ecb07a1"

#BOOHEE_APP_ID = "07f4dffa0c"
#BOOHEE_APP_KEY = "a30bfebe328e516ead6c96afea1cdd28"

BOOHEE_APP_ID = "9438bda1f3"
BOOHEE_APP_KEY = "57f1f2639c667e6ad572c82fd44fafc4"

#建议从https://fc.boohee.com中注册，只需要填写手机号和验证码即可

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