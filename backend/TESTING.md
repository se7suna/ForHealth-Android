# 后端本地测试说明

本文档说明如何在本地环境测试后端 API。

## 前置条件

### 1. 安装 Python 3.11+

```bash
python --version  # 应该显示 3.11 或更高版本
```

### 2. 安装 MongoDB

**Windows (使用 Docker)**:
```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

**或使用本地安装的 MongoDB**

### 3. 安装依赖

```bash
cd backend
pip install -r requirements.txt
```

## 配置环境变量

复制 `.env.example` 为 `.env`：

```bash
cd backend
cp .env.example .env
```

修改 `.env` 文件中的关键配置：

```env
# 必须修改
SECRET_KEY=your-random-secret-key-here

# MongoDB (如果使用 Docker，保持默认即可)
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=for_health

# SMTP 邮件配置（可选，用于测试密码重置）
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM_EMAIL=noreply@forhealth.com
```

**注意**: 如果不配置 SMTP，密码重置功能将无法发送邮件，但其他功能正常。

## 启动服务

### 方式 1: 直接运行

```bash
cd backend
python -m app.main
```

### 方式 2: 使用 uvicorn

```bash
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

服务将运行在: `http://localhost:8000`

## 测试 API

### 1. 查看 API 文档

打开浏览器访问：

- **Swagger UI**: http://localhost:8000/docs （推荐，可直接测试）
- **ReDoc**: http://localhost:8000/redoc

### 2. 健康检查

```bash
curl http://localhost:8000/health
```

预期响应：
```json
{
  "status": "healthy"
}
```

### 3. 完整测试流程

#### 步骤 1: 用户注册

```bash
curl -X POST "http://localhost:8000/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "测试用户",
    "password": "password123"
  }'
```

预期响应 (201):
```json
{
  "message": "注册成功，请继续填写身体基本数据",
  "data": {
    "email": "test@example.com",
    "username": "测试用户"
  }
}
```

#### 步骤 2: 用户登录

```bash
curl -X POST "http://localhost:8000/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

预期响应 (200):
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**保存 access_token，后续请求需要使用！**

#### 步骤 3: 填写身体基本数据

```bash
curl -X POST "http://localhost:8000/api/user/body-data" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "height": 175.0,
    "weight": 70.0,
    "age": 25,
    "gender": "male"
  }'
```

预期响应 (200):
```json
{
  "message": "身体数据更新成功",
  "data": {
    "bmr": 1680.75
  }
}
```

#### 步骤 4: 选择活动水平

```bash
curl -X POST "http://localhost:8000/api/user/activity-level" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "activity_level": "moderately_active"
  }'
```

预期响应 (200):
```json
{
  "message": "活动水平更新成功",
  "data": {
    "activity_level": "moderately_active",
    "tdee": 2605.16
  }
}
```

#### 步骤 5: 设定健康目标

```bash
curl -X POST "http://localhost:8000/api/user/health-goal" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "health_goal_type": "lose_weight",
    "target_weight": 65.0,
    "goal_period_weeks": 10
  }'
```

预期响应 (200):
```json
{
  "message": "健康目标设定成功",
  "data": {
    "health_goal_type": "lose_weight",
    "daily_calorie_goal": 2105.16
  }
}
```

#### 步骤 6: 获取完整资料

```bash
curl -X GET "http://localhost:8000/api/user/profile" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

预期响应 (200):
```json
{
  "email": "test@example.com",
  "username": "测试用户",
  "height": 175.0,
  "weight": 70.0,
  "age": 25,
  "gender": "male",
  "activity_level": "moderately_active",
  "health_goal_type": "lose_weight",
  "target_weight": 65.0,
  "goal_period_weeks": 10,
  "bmr": 1680.75,
  "tdee": 2605.16,
  "daily_calorie_goal": 2105.16
}
```

#### 步骤 7: 更新用户资料

```bash
curl -X PUT "http://localhost:8000/api/user/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "weight": 68.0
  }'
```

预期响应 (200):
```json
{
  "email": "test@example.com",
  "username": "测试用户",
  "height": 175.0,
  "weight": 68.0,
  "age": 25,
  "gender": "male",
  "activity_level": "moderately_active",
  "health_goal_type": "lose_weight",
  "target_weight": 65.0,
  "goal_period_weeks": 10,
  "bmr": 1660.75,
  "tdee": 2574.16,
  "daily_calorie_goal": 2074.16
}
```

### 4. 测试密码重置（需要配置 SMTP）

#### 发送验证码

```bash
curl -X POST "http://localhost:8000/api/auth/password-reset/send-code" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

#### 重置密码

```bash
curl -X POST "http://localhost:8000/api/auth/password-reset/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "verification_code": "123456",
    "new_password": "newpassword123",
    "confirm_password": "newpassword123"
  }'
```

## 运行单元测试

```bash
cd backend
pytest tests/ -v
```

## 常见问题

### 1. MongoDB 连接失败

**错误**: `ConnectionError: Cannot connect to MongoDB`

**解决**:
- 确保 MongoDB 服务正在运行
- 检查 `.env` 中的 `MONGODB_URL` 配置
- 如果使用 Docker: `docker ps` 检查容器是否运行

### 2. 依赖安装失败

**错误**: `pip install` 失败

**解决**:
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 3. 端口被占用

**错误**: `Address already in use`

**解决**:
- 修改 `.env` 中的 `PORT` 配置
- 或杀死占用 8000 端口的进程

### 4. JWT Token 无效

**错误**: `无效的认证凭证`

**解决**:
- Token 可能已过期（默认30分钟），重新登录获取新 Token
- 确保 Header 格式正确: `Authorization: Bearer <token>`

## 数据验证测试

### 测试参数验证

#### 身高超出范围（应失败）

```bash
curl -X POST "http://localhost:8000/api/user/body-data" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "height": 300.0,
    "weight": 70.0,
    "age": 25,
    "gender": "male"
  }'
```

预期响应 (422):
```json
{
  "detail": [
    {
      "loc": ["body", "height"],
      "msg": "ensure this value is less than or equal to 250",
      "type": "value_error.number.not_le"
    }
  ]
}
```

#### 密码过短（应失败）

```bash
curl -X POST "http://localhost:8000/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "username": "用户2",
    "password": "123"
  }'
```

预期响应 (422):
```json
{
  "detail": [
    {
      "loc": ["body", "password"],
      "msg": "ensure this value has at least 6 characters",
      "type": "value_error.any_str.min_length"
    }
  ]
}
```

## 清理测试数据

### 删除 MongoDB 测试数据

```bash
# 连接到 MongoDB
mongo for_health

# 删除 users 集合
db.users.drop()
```

或使用 Docker 重启：

```bash
docker restart mongodb
```

## 测试检查清单

- [x] 服务启动成功
- [x] 健康检查接口正常
- [x] 用户注册功能正常
- [x] 用户登录功能正常，能获取 Token
- [x] 身体基本数据更新正常，BMR 计算正确
- [x] 活动水平更新正常，TDEE 计算正确
- [x] 健康目标设定正常，每日卡路里目标计算正确
- [x] 用户资料获取正常
- [x] 用户资料更新正常，相关数值自动重新计算
- [ ] 密码重置功能正常（需配置 SMTP）
- [x] 参数验证正常，错误提示清晰
- [x] JWT 认证正常，未认证请求被拒绝

## 性能测试

### 并发测试

使用 Apache Bench 进行简单的并发测试：

```bash
# 安装 ab
# Ubuntu: apt-get install apache2-utils
# macOS: 已预装
# Windows: 下载 Apache HTTP Server

# 测试健康检查接口（1000 请求，100 并发）
ab -n 1000 -c 100 http://localhost:8000/health
```

## 下一步

测试通过后，即可提交代码并创建 Merge Request。
