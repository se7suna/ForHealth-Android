# for_health

## Name
就要健康-卡路里消耗记录系统

## Description
一个记录食物卡路里摄入（收入）和运动卡路里消耗（支出）的类记账系统，为扩展功能加入了AI识别与健康助手。

## 当前任务
- [ ] milestone1遗留任务，完成发送验证码的功能
- [ ] milestone1遗留任务，重置密码
- [ ] 整理目录结构
- [ ] 搭建服务器，成功部署后端
- [ ] 完成milestone2所有的用户故事，负责人：李尚格，梁家瑞，黄詹鑫
所有任务应在milestone2开始后的两周内全部提交

## 后端说明文档

### 技术栈

- **框架**: FastAPI 0.109.0
- **数据库**: MongoDB (motor 异步驱动)
- **认证**: JWT (python-jose)
- **密码加密**: BCrypt
- **邮件发送**: aiosmtplib

### 目录结构
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
```

### 环境搭建
**根目录为/for_health**

```bash
# python版本3.11 命令行运行下方命令下载所有依赖
cd backend
pip install -r requirements.txt
```

```bash
# 复制 `.env.example` 为 `.env`
cd backend
copy .env.example .env
```

可修改 `.env` 文件中的配置
```
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

### 本地调试

```bash
# docker运行mongodb数据库
docker run -d -p 27017:27017 --name mongodb mongo:latest
# 启动后端
cd backend
python -m app.main
```
应用将运行在 `http://localhost:8000`

FastAPI 自动生成交互式 API 文档：
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

#### 删除 MongoDB 测试数据

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
#### 数据库操作

使用 motor 异步驱动操作 MongoDB：

```python
from app.database import get_database

db = get_database()
result = await db.collection_name.find_one({"key": "value"})
```

### 开发指南

#### 业务逻辑

##### BMR 计算（Mifflin-St Jeor 公式）

- **男性**: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
- **女性**: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161

##### TDEE 计算

TDEE = BMR × PAL系数

| 活动水平 | PAL 系数 |
|---------|---------|
| 久坐 (sedentary) | 1.2 |
| 轻度活动 (lightly_active) | 1.375 |
| 中度活动 (moderately_active) | 1.55 |
| 重度活动 (very_active) | 1.725 |
| 极重度活动 (extremely_active) | 1.9 |

##### 每日卡路里目标

- **减重**: TDEE - 500 卡
- **增重**: TDEE + 500 卡
- **保持体重**: TDEE

#### API 端点

##### 认证相关 (`/api/auth`)

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/register` | 用户注册 | ❌ |
| POST | `/auth/login` | 用户登录 | ❌ |
| POST | `/auth/password-reset/send-code` | 发送密码重置验证码 | ❌ |
| POST | `/auth/password-reset/verify` | 验证验证码并重置密码 | ❌ |

##### 用户管理 (`/api/user`)

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | `/user/body-data` | 更新身体基本数据 | ✅ |
| POST | `/user/activity-level` | 更新活动水平 | ✅ |
| POST | `/user/health-goal` | 设定健康目标 | ✅ |
| GET | `/user/profile` | 获取用户完整资料 | ✅ |
| PUT | `/user/profile` | 更新用户资料 | ✅ |

#### 添加新的 API 端点

1. 在 `app/schemas/` 中定义请求/响应模型
2. 在 `app/services/` 中实现业务逻辑
3. 在 `app/routers/` 中创建路由端点
4. 在 `app/main.py` 中注册路由

#### 认证机制

使用 JWT Bearer Token 认证：

1. 调用 `/auth/login` 获取 access token
2. 在后续请求的 Header 中添加：
   ```
   Authorization: Bearer <access_token>
   ```

## 前端说明文档

### 技术栈
- **前端**：Android (Kotlin)  
- **UI**：LinearLayout + NumberPicker + Button + TextView  
- **网络层**：Retrofit（模拟网络请求）  
- **协程**：Kotlin Coroutines (Dispatchers.IO / Main)  
- **数据存储**：本地内存模拟 token 保存

### 项目目录结构

```
frontend/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml            # 应用的清单文件，定义Activity、权限、启动页等
│   │   │   ├── java/com/example/forhealth/    # Kotlin 源码目录（包名根据你的项目改）
│   │   │   │   ├── MainActivity.kt            # 应用主入口（App启动页）
│   │   │   │   ├── auth/                      # 用户认证模块
│   │   │   │   │   ├── LoginActivity.kt       # 登录界面逻辑
│   │   │   │   │   └── RegisterActivity.kt    # 注册界面逻辑
│   │   │   │   ├── user/                      # 用户数据模块
│   │   │   │   │   ├── BodyDataActivity.kt    # 录入身体数据界面
│   │   │   │   │   ├── ActivityLevelActivity.kt # 选择活动水平界面
│   │   │   │   │   └── HealthGoalActivity.kt  # 设定健康目标界面
│   │   │   │   ├── network/                   # 网络通信模块
│   │   │   │   │   ├── ApiService.kt          # Retrofit API 接口定义
│   │   │   │   │   └── RetrofitClient.kt      # Retrofit 客户端配置（设置 Base URL、JWT Header 等）
│   │   │   │   ├── model/                     # 数据模型
│   │   │   │   │   ├── User.kt                # 用户数据模型
│   │   │   │   │   └── TokenResponse.kt       # 登录后返回的Token模型
│   │   │   │   ├── utils/                     # 工具类
│   │   │   │   │   ├── PrefsHelper.kt         # SharedPreferences 存储Token等信息
│   │   │   │   │   └── Constants.kt           # 常量定义（例如API_BASE_URL）
│   │   │   │   └── ui/                        # UI层（可选）
│   │   │   │       └── components/            # 可复用的自定义控件
│   │   │   ├── res/                           # 资源文件（res = resources）
│   │   │   │   ├── layout/                    # 页面布局 XML
│   │   │   │   │   ├── activity_login.xml     # 登录页面布局
│   │   │   │   │   ├── activity_register.xml  # 注册页面布局
│   │   │   │   │   ├── activity_body_data.xml # 身体数据录入页面布局
|   |   |   |   |   ├── activity_activity_level.xml  # 活动水平选择页面布局
│   │   │   │   │   └── activity_health_goal.xml # 健康目标页面布局
│   │   │   │   ├── values/                    # 各类资源值
│   │   │   │   │   ├── colors.xml             # 颜色配置（可在这里自定义绿色RGB）
│   │   │   │   │   ├── strings.xml            # 字符串常量
│   │   │   │   │   └── styles.xml             # 全局UI样式（字体、主题色、圆角风格）
│   │   │   │   └── drawable/                  # 可绘制资源（图标、背景、圆角边框等）
│   │   │   │       ├── bg_button_green.xml    # 绿色圆角按钮背景
│   │   │   │       └── bg_input_field.xml     # 输入框圆角背景
│   │   │   └── assets/                        # 静态资源（若需要）
│   │   ├── test/                              # 单元测试
│   │   └── androidTest/                       # 仿真测试
│   ├── build.gradle.kts                       # 模块级构建配置文件（依赖库、编译配置）
├── build.gradle.kts                           # 项目级构建配置文件
├── settings.gradle.kts                        # 项目模块注册
```

### 核心功能
1. **用户登录**
   - 模拟登录功能
   - 登录成功后跳转到身体信息录入界面
   - 登录失败或网络异常会弹出 `Toast` 提示

2. **身体数据录入**
   - 包含 **性别、生日（年月日）、身高、体重** 四个步骤
   - 支持滑动选择，每个步骤有独立 NumberPicker
   - 支持“确定”按钮提交当前数据并进入下一步
   - 数据录入完成后跳转到活动水平选择界面
   - 界面支持滑动动画切换步骤，交互体验平滑

3. **活动水平录入**
   - 提供五个活动水平选项：久坐、轻度活跃、中度活跃、非常活跃、极其活跃
   - 每个选项下方显示对应提示文字（如久坐 → 基本不运动）
   - 用户滑动选择并点击“确定”按钮提交

4. **健康目标录入**
   - 用户选择目标类型（减重/维持体重/增重）、目标体重（kg）、目标周期（周）
   - 每个 NumberPicker 上方有提示文字
   - 用户点击“确定”按钮提交目标

### 本地调试

安装andriod studio 打开frontend文件夹，将自动配置环境依赖，等待安装完成后运行即可测试
**注意**：路径中不允许有中文路径，安装过程中会报错






  