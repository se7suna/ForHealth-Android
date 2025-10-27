# For Health 前后端 API 协作文档

版本：v1.1.0
更新时间：2025-10-27
后端负责人：hayasiakane

## 目录

1. [基础信息](#基础信息)
2. [认证机制](#认证机制)
3. [API 端点](#api-端点)
4. [数据模型](#数据模型)
5. [错误处理](#错误处理)
6. [业务流程](#业务流程)

---

## 基础信息

### 服务地址

- **开发环境**: `http://localhost:8000`
- **生产环境**: TBD

### API 文档

- **Swagger UI**: `http://localhost:8000/docs` （推荐，可直接测试）
- **ReDoc**: `http://localhost:8000/redoc` （更适合阅读）

### 技术栈

- **后端框架**: FastAPI
- **数据库**: MongoDB
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON

---

## 认证机制

### JWT Token 认证

除了注册和登录接口，其他接口都需要在 HTTP Header 中携带 JWT Token：

```http
Authorization: Bearer <access_token>
```

### 获取 Token

通过登录接口获取：

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

# 响应
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### Token 使用示例

```bash
GET /api/user/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## API 端点

### 1. 用户注册

**端点**: `POST /api/auth/register`
**认证**: ❌ 不需要
**说明**: 新用户注册

#### 请求参数

```json
{
  "email": "user@example.com",
  "username": "张三",
  "password": "password123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | ✅ | 邮箱地址（必须是有效格式） |
| username | string | ✅ | 用户名（2-50个字符） |
| password | string | ✅ | 密码（至少6个字符） |

#### 响应示例

**成功 (201)**:
```json
{
  "message": "注册成功，请继续填写身体基本数据",
  "data": {
    "email": "user@example.com",
    "username": "张三"
  }
}
```

**失败 (409)**:
```json
{
  "detail": "该邮箱已被注册"
}
```

---

### 2. 用户登录

**端点**: `POST /api/auth/login`
**认证**: ❌ 不需要
**说明**: 用户登录获取 JWT Token

#### 请求参数

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### 响应示例

**成功 (200)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**失败 (404/401)**:
```json
{
  "detail": "邮箱或密码错误"
}
```

---

### 3. 更新身体基本数据

**端点**: `POST /api/user/body-data`
**认证**: ✅ 需要 JWT Token
**说明**: 提交用户身体基本数据，系统会自动根据出生日期计算年龄，并计算 BMR

#### 请求参数

```json
{
  "height": 175.0,
  "weight": 70.0,
  "birthdate": "1998-05-15",
  "gender": "male"
}
```

| 字段 | 类型 | 必填 | 范围/格式 | 说明 |
|------|------|------|------|------|
| height | float | ✅ | 50-250 | 身高（厘米） |
| weight | float | ✅ | 20-300 | 体重（公斤） |
| birthdate | string | ✅ | YYYY-MM-DD | 出生日期 |
| gender | string | ✅ | male/female | 性别 |

**注意**：
- 出生日期格式必须为 `YYYY-MM-DD`（例如：1998-05-15）
- 系统会根据出生日期自动计算年龄（周岁）
- 计算出的年龄必须在 10-120 岁之间

#### 响应示例

**成功 (200)**:
```json
{
  "message": "身体数据更新成功",
  "data": {
    "bmr": 1680.75
  }
}
```

---

### 4. 更新活动水平

**端点**: `POST /api/user/activity-level`
**认证**: ✅ 需要 JWT Token
**说明**: 选择日常活动水平，系统会自动计算 TDEE

**前置条件**: 必须先完成身体基本数据收集

#### 请求参数

```json
{
  "activity_level": "moderately_active"
}
```

| 字段 | 类型 | 必填 | 可选值 | 说明 |
|------|------|------|--------|------|
| activity_level | string | ✅ | 见下表 | 活动水平 |

**活动水平枚举值**:

| 值 | 中文 | PAL系数 | 说明 |
|----|------|---------|------|
| sedentary | 久坐 | 1.2 | 很少或不运动 |
| lightly_active | 轻度活动 | 1.375 | 每周运动1-3天 |
| moderately_active | 中度活动 | 1.55 | 每周运动3-5天 |
| very_active | 重度活动 | 1.725 | 每周运动6-7天 |
| extremely_active | 极重度活动 | 1.9 | 体力劳动或每天高强度训练 |

#### 响应示例

**成功 (200)**:
```json
{
  "message": "活动水平更新成功",
  "data": {
    "activity_level": "moderately_active",
    "tdee": 2605.16
  }
}
```

**失败 (400)**:
```json
{
  "detail": "请先完成身体基本数据收集"
}
```

---

### 5. 设定健康目标

**端点**: `POST /api/user/health-goal`
**认证**: ✅ 需要 JWT Token
**说明**: 设定健康目标，系统会自动计算每日卡路里目标

**前置条件**: 必须先完成活动水平选择

#### 请求参数

**减重/增重目标**:
```json
{
  "health_goal_type": "lose_weight",
  "target_weight": 65.0,
  "goal_period_weeks": 10
}
```

**保持体重目标**:
```json
{
  "health_goal_type": "maintain_weight"
}
```

| 字段 | 类型 | 必填 | 范围 | 说明 |
|------|------|------|------|------|
| health_goal_type | string | ✅ | lose_weight/gain_weight/maintain_weight | 健康目标类型 |
| target_weight | float | 条件 | 20-300 | 目标体重（减重/增重时必填） |
| goal_period_weeks | int | 条件 | 1-104 | 目标周期周数（减重/增重时必填） |

**健康目标类型**:

| 值 | 中文 | 卡路里计算 |
|----|------|-----------|
| lose_weight | 减重 | TDEE - 500 |
| gain_weight | 增重 | TDEE + 500 |
| maintain_weight | 保持体重 | TDEE |

#### 响应示例

**成功 (200)**:
```json
{
  "message": "健康目标设定成功",
  "data": {
    "health_goal_type": "lose_weight",
    "daily_calorie_goal": 2105.16
  }
}
```

---

### 6. 获取用户资料

**端点**: `GET /api/user/profile`
**认证**: ✅ 需要 JWT Token
**说明**: 获取用户完整资料

#### 响应示例

**成功 (200)**:
```json
{
  "email": "user@example.com",
  "username": "张三",
  "height": 175.0,
  "weight": 70.0,
  "age": 27,
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

**注意**：
- 响应中的 `age` 是根据用户出生日期动态计算的当前年龄（周岁）
- 每次请求都会返回最新计算的年龄

---

### 7. 更新用户资料

**端点**: `PUT /api/user/profile`
**认证**: ✅ 需要 JWT Token
**说明**: 更新用户资料，所有字段可选，系统会自动重新计算相关数值

#### 请求参数

```json
{
  "username": "李四",
  "weight": 68.0,
  "birthdate": "1998-08-20",
  "activity_level": "very_active"
}
```

所有字段都是可选的，只需要传递需要更新的字段。

**可更新字段**：
- `username`: 用户名
- `height`: 身高
- `weight`: 体重
- `birthdate`: 出生日期（格式：YYYY-MM-DD）
- `gender`: 性别
- `activity_level`: 活动水平
- `health_goal_type`: 健康目标类型
- `target_weight`: 目标体重
- `goal_period_weeks`: 目标周期

**注意**：
- 如果更新了 `birthdate`，系统会自动重新计算年龄
- 如果修改了相关字段（身高、体重、出生日期等），系统会自动重新计算 BMR、TDEE 和每日卡路里目标

#### 响应示例

**成功 (200)**:
```json
{
  "email": "user@example.com",
  "username": "李四",
  "height": 175.0,
  "weight": 68.0,
  "age": 27,
  "gender": "male",
  "activity_level": "very_active",
  "health_goal_type": "lose_weight",
  "target_weight": 65.0,
  "goal_period_weeks": 10,
  "bmr": 1660.75,
  "tdee": 2864.79,
  "daily_calorie_goal": 2364.79
}
```

---

### 8. 发送密码重置验证码

**端点**: `POST /api/auth/password-reset/send-code`
**认证**: ❌ 不需要
**说明**: 向用户邮箱发送6位数字验证码，有效期5分钟

#### 请求参数

```json
{
  "email": "user@example.com"
}
```

#### 响应示例

**成功 (200)**:
```json
{
  "message": "验证码已发送至邮箱，有效期5分钟"
}
```

**失败 (404)**:
```json
{
  "detail": "该邮箱未注册"
}
```

---

### 9. 验证码重置密码

**端点**: `POST /api/auth/password-reset/verify`
**认证**: ❌ 不需要
**说明**: 使用验证码重置密码

#### 请求参数

```json
{
  "email": "user@example.com",
  "verification_code": "123456",
  "new_password": "newpassword123",
  "confirm_password": "newpassword123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | ✅ | 邮箱地址 |
| verification_code | string | ✅ | 6位数字验证码 |
| new_password | string | ✅ | 新密码（至少6个字符） |
| confirm_password | string | ✅ | 确认新密码（必须与新密码一致） |

#### 响应示例

**成功 (200)**:
```json
{
  "message": "密码重置成功，请使用新密码登录"
}
```

**失败 (400)**:
```json
{
  "detail": "验证码错误或已过期"
}
```

---

## 数据模型

### Gender（性别）

```typescript
type Gender = "male" | "female";
```

### ActivityLevel（活动水平）

```typescript
type ActivityLevel =
  | "sedentary"           // 久坐
  | "lightly_active"      // 轻度活动
  | "moderately_active"   // 中度活动
  | "very_active"         // 重度活动
  | "extremely_active";   // 极重度活动
```

### HealthGoalType（健康目标类型）

```typescript
type HealthGoalType =
  | "lose_weight"      // 减重
  | "gain_weight"      // 增重
  | "maintain_weight"; // 保持体重
```

### UserProfile（用户资料）

```typescript
interface UserProfile {
  email: string;
  username: string;
  height?: number;              // 身高（厘米）
  weight?: number;              // 体重（公斤）
  age?: number;                 // 年龄（周岁，由后端根据出生日期动态计算）
  gender?: Gender;              // 性别
  activity_level?: ActivityLevel;  // 活动水平
  health_goal_type?: HealthGoalType; // 健康目标类型
  target_weight?: number;       // 目标体重（公斤）
  goal_period_weeks?: number;   // 目标周期（周）
  bmr?: number;                 // 基础代谢率（卡路里/天）
  tdee?: number;                // 每日总能量消耗（卡路里/天）
  daily_calorie_goal?: number;  // 每日卡路里目标（卡路里/天）
}
```

**重要说明**：
- 前端在提交身体数据时，需要发送 `birthdate`（出生日期，格式：YYYY-MM-DD）
- 后端会根据 `birthdate` 自动计算当前年龄（周岁）
- 前端在获取用户资料时，可以直接读取已计算好的 `age` 字段
- 数据库同时存储 `birthdate` 和 `age` 两个字段

---

## 错误处理

### 标准错误响应格式

```json
{
  "detail": "错误描述信息"
}
```

### HTTP 状态码

| 状态码 | 说明 | 常见场景 |
|--------|------|----------|
| 200 | 成功 | GET、PUT 请求成功 |
| 201 | 创建成功 | POST 创建资源成功 |
| 400 | 请求参数错误 | 参数验证失败、业务逻辑错误 |
| 401 | 未授权 | Token 无效或过期 |
| 404 | 资源不存在 | 用户不存在、邮箱未注册 |
| 409 | 冲突 | 邮箱已被注册 |
| 500 | 服务器内部错误 | 服务器异常 |

### 常见错误示例

**参数验证错误 (422)**:
```json
{
  "detail": [
    {
      "loc": ["body", "height"],
      "msg": "身高必须在 50-250cm 之间",
      "type": "value_error"
    }
  ]
}
```

**认证失败 (401)**:
```json
{
  "detail": "无效的认证凭证"
}
```

---

## 业务流程

### 新用户完整注册流程

```mermaid
graph TD
    A[1. 用户注册] --> B[2. 填写身体基本数据]
    B --> C[3. 选择活动水平]
    C --> D[4. 设定健康目标]
    D --> E[完成初始化]
```

#### 详细步骤

1. **用户注册** (`POST /api/auth/register`)
   - 提供邮箱、用户名、密码
   - 获取注册成功响应

2. **用户登录** (`POST /api/auth/login`)
   - 使用邮箱和密码登录
   - 获取 JWT Token

3. **填写身体基本数据** (`POST /api/user/body-data`)
   - 提供身高、体重、出生日期、性别
   - 系统自动计算年龄和 BMR

4. **选择活动水平** (`POST /api/user/activity-level`)
   - 选择日常活动水平
   - 系统计算 TDEE

5. **设定健康目标** (`POST /api/user/health-goal`)
   - 选择健康目标类型
   - 如果是减重/增重，提供目标体重和周期
   - 系统计算每日卡路里目标

6. **查看完整资料** (`GET /api/user/profile`)
   - 获取所有个人信息和计算结果

### 密码重置流程

```mermaid
graph TD
    A[1. 发送验证码] --> B[2. 用户查收邮件]
    B --> C[3. 输入验证码和新密码]
    C --> D[4. 密码重置成功]
    D --> E[5. 使用新密码登录]
```

---

## 前端开发建议

### 1. 状态管理

建议在前端维护以下状态：

```typescript
interface AuthState {
  isAuthenticated: boolean;
  accessToken: string | null;
  user: UserProfile | null;
}
```

### 2. API 请求封装

建议封装一个统一的 API 请求函数，自动添加 Token：

```typescript
async function apiRequest(url: string, options: RequestInit = {}) {
  const token = localStorage.getItem('accessToken');

  const headers = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options.headers,
  };

  const response = await fetch(`http://localhost:8000${url}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.detail);
  }

  return response.json();
}
```

### 3. 表单验证

前端应实现与后端一致的验证规则：

- 邮箱格式验证
- 密码长度验证（至少6个字符）
- 数值范围验证（身高、体重等）
- 出生日期格式验证（YYYY-MM-DD）
- 出生日期合理性验证（计算出的年龄应在 10-120 岁之间）
- 两次密码输入一致性验证

### 4. 错误处理

统一处理 API 错误，提供友好的用户提示：

```typescript
try {
  const data = await apiRequest('/api/user/profile');
  // 处理成功响应
} catch (error) {
  if (error.message.includes('无效的认证凭证')) {
    // Token 失效，跳转到登录页
    router.push('/login');
  } else {
    // 显示错误提示
    showToast(error.message);
  }
}
```

### 5. 加载状态

为异步请求添加加载状态，提升用户体验：

```typescript
const [loading, setLoading] = useState(false);

async function handleSubmit() {
  setLoading(true);
  try {
    await apiRequest('/api/user/body-data', {
      method: 'POST',
      body: JSON.stringify(formData),
    });
  } finally {
    setLoading(false);
  }
}
```

---

## 联系方式

- **后端负责人**: hayasiakane
- **GitLab 项目**: https://gitlab.com/tj-cs-swe/CS10102302-2025/group12/for_health
- **相关 Issues**: #5, #6, #7, #8

---

## 更新日志

### v1.1.0 (2025-10-27)

- **重大变更**：身体数据提交方式调整
  - 前端提交 `birthdate`（出生日期）代替 `age`（年龄）
  - 后端自动根据出生日期计算年龄（周岁）
  - 数据库同时存储出生日期和计算后的年龄
  - 前端可直接读取已计算好的年龄字段
- 优化年龄计算逻辑，确保准确计算周岁
- 更新所有相关 API 文档和数据模型说明

### v1.0.0 (2025-10-27)

- 初始版本
- 实现账户信息管理模块所有 API
- 支持用户注册、登录、身体数据管理、活动水平设置、健康目标设定
- 支持密码重置功能
- 自动计算 BMR、TDEE 和每日卡路里目标
