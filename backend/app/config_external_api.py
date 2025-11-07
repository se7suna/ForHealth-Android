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
BOOHEE_APP_ID = "69cbee103b"
BOOHEE_APP_KEY = "122e5732e99a722611abaa5573ba5edc"

# 由于每天单个账号有限制，所以需要使用多个应用ID和应用密钥
# 备用id和key
#AppID  5c44cee028
#AppKey 3d5c25cfe7b2a33ae104d0f36ecb07a1

#AppID    07f4dffa0c
#AppKey a30bfebe328e516ead6c96afea1cdd28

#建议从https://fc.boohee.com中注册，只需要填写手机号和验证码即可

# AccessToken缓存时间（秒），默认1小时
# 注意：实际Token有效期约为1个月，这里设置缓存时间用于本地缓存管理
BOOHEE_TOKEN_CACHE_TIME = 3600

# API请求超时时间（秒）
EXTERNAL_API_TIMEOUT = 10

# 是否启用外部API（用于测试时关闭）
EXTERNAL_API_ENABLED = True

