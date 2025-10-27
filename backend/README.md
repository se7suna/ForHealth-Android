# For Health 后端 API

基于 FastAPI 的卡路里消耗记录系统后端服务。

## 技术栈

- **框架**: FastAPI 0.109.0
- **数据库**: MongoDB (motor 异步驱动)
- **认证**: JWT (python-jose)
- **密码加密**: BCrypt
- **邮件发送**: aiosmtplib

## 项目结构

```
backend/
├── app/
│   ├── main.py              # FastAPI 应用入口
│   ├── config.py            # 配置管理
│   ├── database.py          # 数据库连接
│   ├── models/              # 数据库模型
│   │   └── user.py          # 用户模型
│   ├── schemas/             # Pydantic schemas (请求/响应)
│   │   └── user.py          # 用户相关 schemas
│   ├── routers/             # API 路由
│   │   ├── auth.py          # 认证路由
│   │   └── user.py          # 用户管理路由
│   ├── services/            # 业务逻辑层
│   │   ├── calculation_service.py  # BMR/TDEE 计算
│   │   ├── auth_service.py         # 认证服务
│   │   └── user_service.py         # 用户服务
│   └── utils/               # 工具函数
│       ├── security.py      # 密码加密、JWT
│       └── email.py         # 邮件发送
├── tests/                   # 测试
├── requirements.txt         # Python 依赖
├── .env.example            # 环境变量示例
└── README.md               # 本文档
```

## 快速开始

### 1. 安装依赖

```bash
cd backend
pip install -r requirements.txt
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并填写配置：

```bash
cp .env.example .env
```

必须配置的关键项：
- `SECRET_KEY`: JWT 密钥（生产环境请使用强随机密钥）
- `MONGODB_URL`: MongoDB 连接地址
- `SMTP_*`: 邮件服务器配置（用于密码重置）

### 3. 启动 MongoDB

确保 MongoDB 服务正在运行：

```bash
# Docker 方式
docker run -d -p 27017:27017 --name mongodb mongo:latest

# 或使用本地安装的 MongoDB
mongod
```

### 4. 运行应用

```bash
# 开发模式（自动重载）
python -m app.main

# 或使用 uvicorn
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

应用将运行在 `http://localhost:8000`

### 5. 访问 API 文档

FastAPI 自动生成交互式 API 文档：

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API 端点

### 认证相关 (`/api/auth`)

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/register` | 用户注册 | ❌ |
| POST | `/auth/login` | 用户登录 | ❌ |
| POST | `/auth/password-reset/send-code` | 发送密码重置验证码 | ❌ |
| POST | `/auth/password-reset/verify` | 验证验证码并重置密码 | ❌ |

### 用户管理 (`/api/user`)

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | `/user/body-data` | 更新身体基本数据 | ✅ |
| POST | `/user/activity-level` | 更新活动水平 | ✅ |
| POST | `/user/health-goal` | 设定健康目标 | ✅ |
| GET | `/user/profile` | 获取用户完整资料 | ✅ |
| PUT | `/user/profile` | 更新用户资料 | ✅ |

## 认证机制

使用 JWT Bearer Token 认证：

1. 调用 `/auth/login` 获取 access token
2. 在后续请求的 Header 中添加：
   ```
   Authorization: Bearer <access_token>
   ```

## 业务逻辑

### BMR 计算（Mifflin-St Jeor 公式）

- **男性**: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
- **女性**: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161

### TDEE 计算

TDEE = BMR × PAL系数

| 活动水平 | PAL 系数 |
|---------|---------|
| 久坐 (sedentary) | 1.2 |
| 轻度活动 (lightly_active) | 1.375 |
| 中度活动 (moderately_active) | 1.55 |
| 重度活动 (very_active) | 1.725 |
| 极重度活动 (extremely_active) | 1.9 |

### 每日卡路里目标

- **减重**: TDEE - 500 卡
- **增重**: TDEE + 500 卡
- **保持体重**: TDEE

## 测试

```bash
# 运行测试
pytest

# 运行测试并查看覆盖率
pytest --cov=app tests/
```

## 开发指南

### 添加新的 API 端点

1. 在 `app/schemas/` 中定义请求/响应模型
2. 在 `app/services/` 中实现业务逻辑
3. 在 `app/routers/` 中创建路由端点
4. 在 `app/main.py` 中注册路由

### 数据库操作

使用 motor 异步驱动操作 MongoDB：

```python
from app.database import get_database

db = get_database()
result = await db.collection_name.find_one({"key": "value"})
```

## 部署

### 使用 Docker

```bash
# 构建镜像
docker build -t for-health-backend .

# 运行容器
docker run -d -p 8000:8000 --env-file .env for-health-backend
```

### 生产环境注意事项

1. 使用强随机密钥作为 `SECRET_KEY`
2. 关闭 `DEBUG` 模式
3. 配置正确的 `ALLOWED_ORIGINS`
4. 使用 HTTPS
5. 使用 Redis 存储验证码（当前使用内存存储）

## 许可证

MIT License
