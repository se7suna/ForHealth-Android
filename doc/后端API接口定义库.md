# For Health 前后端 API 协作文档

版本：v2.3.0
更新时间：2025-11-30
后端负责人：hayasiakane

## 📌 最新更新 (2025-11-30)

### ⚠️ 前端同学必读 - 重要变更

#### 1. **登录接口返回值变更** 🔴 Breaking Change
- **接口**: `POST /api/auth/login`
- **变更**: 响应中新增 `refresh_token` 字段
- **影响**: 前端需要同时保存 `access_token` 和 `refresh_token`
- **操作指南**: 详见 [Refresh Token 机制](#新增-refresh-token-自动刷新机制)

#### 2. **新增 Token 刷新接口** 🆕
- **接口**: `POST /api/auth/refresh`
- **用途**: 使用 refresh_token 获取新的 access_token
- **必须实现**: 前端需配置 Axios 拦截器自动刷新过期 token
- **参考文档**: `backend/REFRESH_TOKEN_GUIDE.md`

#### 3. **Token 有效期调整** ⏰
- **Access Token**: 从 30 分钟改为 **15 分钟**
- **Refresh Token**: 新增，有效期 **30 天**
- **影响**: 用户登录一次后 30 天内无需重新登录

#### 4. **体重字段自动同步** 🔄
- 创建体重记录会自动更新用户当前体重
- 更新用户体重会自动创建历史记录
- **注意**: 前端无需额外操作，后端已自动处理

### 快速上手前端改造
```javascript
// 1. 保存 tokens（登录成功后）
localStorage.setItem('refresh_token', refresh_token);  // 长期存储
sessionStorage.setItem('access_token', access_token);   // 临时存储

// 2. 配置 Axios 拦截器（详见 REFRESH_TOKEN_GUIDE.md）
// 3. API 请求时使用 access_token
// 4. 401 错误时自动用 refresh_token 刷新
```

---

## 目录

1. [基础信息](#基础信息)
2. [认证机制](#认证机制)
3. [用户管理 API](#api-端点) (API 1-9)
4. [食物管理 API](#食物管理-api) (API 10-20)
5. [食谱管理 API](#食谱管理-api) (API 21-31)
6. [条形码扫描 API](#条形码扫描-api) (API 32-33)
7. [运动管理 API](#运动管理-api) (API 34-43)
8. [数据模型](#数据模型)
9. [错误处理](#错误处理)
10. [业务流程](#业务流程)

---

## 基础信息

### 服务地址

- **开发环境**: `http://localhost:8000`
- **生产环境**: TBD

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

## API 快速索引

**图例说明**:
- 🆕 = 今日新增 (2025-11-30)
- 🔄 = 今日更新 (2025-11-30)
- ⚠️ = 重要变更，前端必须适配

### 用户管理 API (14个)

| 序号 | 端点 | 方法 | 说明 | 认证 | 状态 |
|------|------|------|------|------|------|
| 1 | `/api/auth/register` | POST | 用户注册 | ❌ | |
| 2 | `/api/auth/login` | POST | 用户登录 | ❌ | 🔄⚠️ |
| 2.1 | `/api/auth/refresh` | POST | 刷新 Token | ❌ | 🆕⚠️ |
| 3 | `/api/user/body-data` | POST | 更新身体基本数据 | ✅ | 🔄 |
| 4 | `/api/user/activity-level` | POST | 更新活动水平 | ✅ | |
| 5 | `/api/user/health-goal` | POST | 设定健康目标 | ✅ | |
| 6 | `/api/user/profile` | GET | 获取用户资料 | ✅ | |
| 7 | `/api/user/profile` | PUT | 更新用户资料 | ✅ | 🔄 |
| 8 | `/api/auth/password-reset/send-code` | POST | 发送密码重置验证码 | ❌ | |
| 9 | `/api/auth/password-reset/verify` | POST | 验证码重置密码 | ❌ | |
| 10 | `/api/user/weight-record` | POST | 创建体重记录 | ✅ | 🔄 |
| 11 | `/api/user/weight-records` | GET | 获取体重记录列表 | ✅ | |
| 12 | `/api/user/weight-record/{record_id}` | PUT | 更新体重记录 | ✅ | |
| 13 | `/api/user/weight-record/{record_id}` | DELETE | 删除体重记录 | ✅ | |

### 食物管理 API (11个)

#### 食物基础管理 (6个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 10 | `/api/food/` | POST | 创建食物 | ✅ |
| 11 | `/api/food/search` | GET | 搜索食物 | ✅ |
| 12 | `/api/food/search-id` | GET | 按名称搜索食物ID | ✅ |
| 13 | `/api/food/{food_id}` | GET | 获取食物详情 | ✅ |
| 14 | `/api/food/{food_id}` | PUT | 更新食物 | ✅ |
| 15 | `/api/food/{food_id}` | DELETE | 删除食物 | ✅ |

#### 食物记录管理 (5个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 16 | `/api/food/record` | POST | 创建食物记录 | ✅ |
| 17 | `/api/food/record/list` | GET | 获取食物记录列表 | ✅ |
| 18 | `/api/food/record/daily/{date}` | GET | 获取每日营养摘要 | ✅ |
| 19 | `/api/food/record/{record_id}` | PUT | 更新食物记录 | ✅ |
| 20 | `/api/food/record/{record_id}` | DELETE | 删除食物记录 | ✅ |

### 食谱管理 API (11个)

#### 食谱基础管理 (7个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 21 | `/api/recipe/` | POST | 创建食谱 | ✅ |
| 22 | `/api/recipe/search` | GET | 搜索食谱 | ✅ |
| 23 | `/api/recipe/search-id` | GET | 按名称搜索食谱ID | ✅ |
| 24 | `/api/recipe/categories` | GET | 获取食谱分类 | ✅ |
| 25 | `/api/recipe/{recipe_id}` | GET | 获取食谱详情 | ✅ |
| 26 | `/api/recipe/{recipe_id}` | PUT | 更新食谱 | ✅ |
| 27 | `/api/recipe/{recipe_id}` | DELETE | 删除食谱 | ✅ |

#### 食谱记录管理 (4个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 28 | `/api/recipe/record` | POST | 创建食谱记录 | ✅ |
| 29 | `/api/recipe/record` | GET | 获取食谱记录列表 | ✅ |
| 30 | `/api/recipe/record/{batch_id}` | PUT | 更新食谱记录 | ✅ |
| 31 | `/api/recipe/record/{batch_id}` | DELETE | 删除食谱记录 | ✅ |

### 条形码扫描 API (2个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 32 | `/api/food/barcode/recognize` | POST | 从图片识别条形码 | ✅ |
| 33 | `/api/food/barcode/{barcode}` | GET | 扫描条形码查询食品信息 | ✅ |

### 运动管理 API (10个)

#### 运动类型管理 (4个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 34 | `/api/sports/create-sport` | POST | 创建自定义运动类型 | ✅ |
| 35 | `/api/sports/update-sport` | POST | 更新自定义运动类型 | ✅ |
| 36 | `/api/sports/delete-sport/{sport_type}` | GET | 删除自定义运动类型 | ✅ |
| 37 | `/api/sports/get-available-sports-types` | GET | 获取可用运动类型列表 | ✅ |

#### 运动记录管理 (6个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 38 | `/api/sports/log-sports` | POST | 记录运动 | ✅ |
| 39 | `/api/sports/update-sport-record` | POST | 更新运动记录 | ✅ |
| 40 | `/api/sports/delete-sport-record/{record_id}` | GET | 删除运动记录 | ✅ |
| 41 | `/api/sports/search-sports-records` | POST | 搜索运动记录 | ✅ |
| 42 | `/api/sports/get-all-sports-records` | GET | 获取全部运动记录 | ✅ |
| 43 | `/api/sports/sports-report` | GET | 获取运动报告 | ✅ |

### 可视化报告 API (4个)

| 序号 | 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|------|
| 44 | `/api/visualization/daily-calorie-summary` | GET | 获取每日卡路里摘要 | ✅ |
| 45 | `/api/visualization/nutrition-analysis` | GET | 获取营养素与食物来源分析 | ✅ |
| 46 | `/api/visualization/time-series-trend` | GET | 获取时间序列趋势分析 | ✅ |
| 47 | `/api/visualization/export-report` | GET | 导出健康数据报告 | ✅ |

**总计：51个API端点**

---

## API 端点详细说明

### 用户管理 API

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

### 2. 用户登录 🔄⚠️

**端点**: `POST /api/auth/login`
**认证**: ❌ 不需要
**说明**: 用户登录获取 JWT Token（包含 access token 和 refresh token）

**⚠️ 重要变更 (2025-11-30)**:
- 响应中新增 `refresh_token` 字段
- 前端必须同时保存 `access_token` 和 `refresh_token`
- `access_token` 有效期改为 15 分钟
- `refresh_token` 有效期 30 天，用于自动刷新 access token

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
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**字段说明**:
- `access_token`: 短期访问令牌（15 分钟有效），用于 API 请求
- `refresh_token`: 长期刷新令牌（30 天有效），用于刷新 access token
- `token_type`: 令牌类型（固定为 "bearer"）

**前端存储建议**:
```javascript
// 临时存储（关闭浏览器后清除）
sessionStorage.setItem('access_token', access_token);
// 持久存储（30 天内有效）
localStorage.setItem('refresh_token', refresh_token);
```

**失败 (404/401)**:
```json
{
  "detail": "邮箱或密码错误"
}
```

---

### 2.1 刷新 Token 🆕⚠️

**端点**: `POST /api/auth/refresh`
**认证**: ❌ 不需要（但需要有效的 refresh token）
**说明**: 使用 refresh token 获取新的 access token 和 refresh token

**对应文档**: `backend/REFRESH_TOKEN_GUIDE.md` - 完整前端集成指南

#### 使用场景

- Access token 过期（15 分钟后）
- API 返回 401 错误
- 前端拦截器自动调用此接口刷新 token
- 无需用户重新登录

#### 请求参数

```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refresh_token | string | ✅ | 登录时获得的 refresh token |

#### 响应示例

**成功 (200)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**注意**:
- 每次刷新都会返回**新的** access token 和 refresh token
- 旧的 tokens 会立即失效
- 前端需要更新存储的两个 token

**失败 (401)**:
```json
{
  "detail": "无效的 refresh token"
}
```

**失败 (404)**:
```json
{
  "detail": "用户不存在"
}
```

#### 前端集成示例

使用 Axios 拦截器自动刷新：

```javascript
// 响应拦截器 - 处理 401 错误
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // 如果是 401 错误且没有重试过
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('refresh_token');

      try {
        // 调用刷新接口
        const response = await axios.post('/api/auth/refresh', {
          refresh_token: refreshToken
        });

        const { access_token, refresh_token: newRefreshToken } = response.data;

        // 保存新的 tokens
        sessionStorage.setItem('access_token', access_token);
        localStorage.setItem('refresh_token', newRefreshToken);

        // 重试原请求
        originalRequest.headers['Authorization'] = `Bearer ${access_token}`;
        return api(originalRequest);
      } catch (refreshError) {
        // 刷新失败，跳转登录
        localStorage.removeItem('refresh_token');
        sessionStorage.removeItem('access_token');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

**完整实现指南**: 详见 `backend/REFRESH_TOKEN_GUIDE.md`

---

### 3. 更新身体基本数据 🔄

**端点**: `POST /api/user/body-data`
**认证**: ✅ 需要 JWT Token
**说明**: 提交用户身体基本数据，系统会自动根据出生日期计算年龄，并计算 BMR

**🔄 更新说明 (2025-11-30)**:
- 更新体重时会自动创建体重历史记录
- 无需手动调用 `POST /api/user/weight-record` 接口
- 体重记录的 `recorded_at` 时间为当前时间

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
- **体重字段会自动同步**：更新体重时系统会自动在 `weight_records` 表中创建历史记录

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

### 7. 更新用户资料 🔄

**端点**: `PUT /api/user/profile`
**认证**: ✅ 需要 JWT Token
**说明**: 更新用户资料，所有字段可选，系统会自动重新计算相关数值

**🔄 更新说明 (2025-11-30)**:
- 更新体重时会自动创建体重历史记录
- 无需手动调用 `POST /api/user/weight-record` 接口
- 体重记录的 `recorded_at` 时间为当前时间

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
- `weight`: 体重（更新时自动创建历史记录）
- `birthdate`: 出生日期（格式：YYYY-MM-DD）
- `gender`: 性别
- `activity_level`: 活动水平
- `health_goal_type`: 健康目标类型
- `target_weight`: 目标体重
- `goal_period_weeks`: 目标周期

**注意**：
- 如果更新了 `birthdate`，系统会自动重新计算年龄
- 如果修改了相关字段（身高、体重、出生日期等），系统会自动重新计算 BMR、TDEE 和每日卡路里目标
- **体重字段会自动同步**：更新体重时系统会自动在 `weight_records` 表中创建历史记录

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

### 10. 创建体重记录 🔄

**端点**: `POST /api/user/weight-record`
**认证**: ✅ 需要 JWT Token
**说明**: 记录用户的历史体重数据

**对应 Issue**: #79 - 添加历史体重记录功能

**🔄 更新说明 (2025-11-30)**:
- 创建体重记录时会自动更新用户的当前体重（`users.weight` 字段）
- 实现了体重数据的双向同步
- 用户最新的体重始终保持一致

#### 请求参数

```json
{
  "weight": 70.5,
  "recorded_at": "2025-11-24T10:30:00",
  "notes": "晨起空腹"
}
```

| 字段 | 类型 | 必填 | 范围/格式 | 说明 |
|------|------|------|------|------|
| weight | float | ✅ | 0-500 | 体重（公斤） |
| recorded_at | datetime | ✅ | ISO 8601 | 记录时间 |
| notes | string | ❌ | 最多200字符 | 备注 |

**重要提示**：
- 创建记录时，系统会自动将该体重值同步到用户资料的 `weight` 字段
- 前端无需额外调用 `PUT /api/user/profile` 更新用户体重

#### 响应示例

**成功 (201)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "weight": 70.5,
  "recorded_at": "2025-11-24T10:30:00",
  "notes": "晨起空腹",
  "created_at": "2025-11-24T10:30:00"
}
```

**失败 (422) - 无效体重**:
```json
{
  "detail": [
    {
      "loc": ["body", "weight"],
      "msg": "ensure this value is greater than 0",
      "type": "value_error.number.not_gt"
    }
  ]
}
```

---

### 11. 获取体重记录列表

**端点**: `GET /api/user/weight-records`
**认证**: ✅ 需要 JWT Token
**说明**: 获取用户的体重记录列表，支持日期范围筛选

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| start_date | date | ❌ | - | 开始日期（YYYY-MM-DD） |
| end_date | date | ❌ | - | 结束日期（YYYY-MM-DD） |
| limit | integer | ❌ | 100 | 返回数量限制（最大500） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 15,
  "records": [
    {
      "id": "507f1f77bcf86cd799439011",
      "weight": 70.5,
      "recorded_at": "2025-11-24T10:30:00",
      "notes": "晨起空腹",
      "created_at": "2025-11-24T10:30:00"
    },
    {
      "id": "507f1f77bcf86cd799439012",
      "weight": 70.0,
      "recorded_at": "2025-11-23T10:30:00",
      "notes": null,
      "created_at": "2025-11-23T10:30:00"
    }
  ]
}
```

**说明**：
- 记录按 `recorded_at` 时间倒序排列（最新的在前）
- 可以通过 `start_date` 和 `end_date` 筛选特定日期范围
- 用于体重趋势分析和可视化

---

### 12. 更新体重记录

**端点**: `PUT /api/user/weight-record/{record_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 更新已有的体重记录（仅创建者可更新）

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | string | ✅ | 体重记录ID |

#### 请求参数

所有字段可选：

```json
{
  "weight": 71.0,
  "recorded_at": "2025-11-25T10:30:00",
  "notes": "更新后的备注"
}
```

| 字段 | 类型 | 必填 | 范围/格式 | 说明 |
|------|------|------|------|------|
| weight | float | ❌ | 0-500 | 体重（公斤） |
| recorded_at | datetime | ❌ | ISO 8601 | 记录时间 |
| notes | string | ❌ | 最多200字符 | 备注 |

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "weight": 71.0,
  "recorded_at": "2025-11-25T10:30:00",
  "notes": "更新后的备注",
  "created_at": "2025-11-24T10:30:00"
}
```

**失败 (404)**:
```json
{
  "detail": "记录不存在或无权更新"
}
```

---

### 13. 删除体重记录

**端点**: `DELETE /api/user/weight-record/{record_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除指定的体重记录（仅创建者可删除）

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | string | ✅ | 体重记录ID |

#### 响应示例

**成功 (200)**:
```json
{
  "message": "体重记录删除成功"
}
```

**失败 (404)**:
```json
{
  "detail": "记录不存在或无权删除"
}
```

---

## 食物管理 API

### 食物基础管理

### 10. 创建食物

**端点**: `POST /api/food/`
**认证**: ✅ 需要 JWT Token
**说明**: 创建自定义食物信息

#### 请求参数

```json
{
  "name": "测试苹果",
  "category": "水果",
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_per_serving": {
    "calories": 52,
    "protein": 0.3,
    "carbohydrates": 14,
    "fat": 0.2,
    "fiber": 2.4,
    "sugar": 10.4,
    "sodium": 1
  },
  "brand": "测试品牌",
  "barcode": "1234567890123",
  "image_url": "https://example.com/apple.jpg"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 食物名称 |
| category | string | ❌ | 食物分类（水果、蔬菜、肉类等） |
| serving_size | number | ✅ | 标准份量（克） |
| serving_unit | string | ❌ | 份量单位（默认：克） |
| nutrition_per_serving | object | ✅ | 每份基础营养数据 |
| nutrition_per_serving.calories | number | ✅ | 卡路里（千卡） |
| nutrition_per_serving.protein | number | ✅ | 蛋白质（克） |
| nutrition_per_serving.carbohydrates | number | ✅ | 碳水化合物（克） |
| nutrition_per_serving.fat | number | ✅ | 脂肪（克） |
| nutrition_per_serving.fiber | number | ❌ | 膳食纤维（克） |
| nutrition_per_serving.sugar | number | ❌ | 糖分（克） |
| nutrition_per_serving.sodium | number | ❌ | 钠（毫克） |
| brand | string | ❌ | 品牌 |
| barcode | string | ❌ | 条形码 |
| image_url | string | ❌ | 食物图片URL |

**注意**：用户创建的食物仅创建者自己可见，其他用户无法搜索或查看。

#### 响应示例

**成功 (201)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "测试苹果",
  "category": "水果",
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_per_serving": {
    "calories": 52,
    "protein": 0.3,
    "carbohydrates": 14,
    "fat": 0.2,
    "fiber": 2.4,
    "sugar": 10.4,
    "sodium": 1
  },
  "brand": "测试品牌",
  "barcode": "1234567890123",
  "image_url": "https://example.com/apple.jpg",
  "created_by": "user@example.com",
  "created_at": "2024-01-15T08:30:00"
}
```

---

### 11. 搜索食物

**端点**: `GET /api/food/search`
**认证**: ✅ 需要 JWT Token
**说明**: 搜索食物（调用薄荷健康官方数据库）

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | ❌ | - | 搜索关键词 |
| page | integer | ❌ | 1 | 页码（每页30条，最多10页） |
| include_full_nutrition | boolean | ❌ | false | 是否获取完整营养信息 |
| simplified | boolean | ❌ | false | 是否返回简化版本 |

#### 响应示例

**成功 (200) - 完整版本**:
```json
{
  "page": 1,
  "total_pages": 5,
  "foods": [
    {
      "source": "boohee",
      "food_id": null,
      "boohee_id": "12345",
      "code": "abc123",
      "name": "苹果",
      "weight": 100,
      "weight_unit": "克",
      "brand": null,
      "image_url": "https://...",
      "nutrition": {
        "calories": 52,
        "protein": 0.3,
        "carbohydrates": 14,
        "fat": 0.2
      }
    }
  ]
}
```

**成功 (200) - 简化版本**:
```json
{
  "page": 1,
  "total_pages": 5,
  "foods": [
    {
      "source": "boohee",
      "name": "苹果",
      "weight": 100,
      "weight_unit": "克",
      "nutrition": {
        "calories": 52,
        "protein": 0.3,
        "fat": 0.2,
        "carbohydrates": 14,
        "sugar": 10.4,
        "sodium": 1
      }
    }
  ]
}
```

---

### 12. 按名称搜索食物ID

**端点**: `GET /api/food/search-id`
**认证**: ✅ 需要 JWT Token
**说明**: 通过食物名称搜索本地数据库，返回ID和名称（用于快速查找）

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | ✅ | - | 搜索关键词 |
| limit | integer | ❌ | 20 | 返回数量限制（最大100） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 5,
  "foods": [
    {
      "id": "507f1f77bcf86cd799439011",
      "name": "苹果",
      "category": "水果"
    }
  ]
}
```

---

### 13. 获取食物详情

**端点**: `GET /api/food/{food_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定食物的详细信息

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "苹果",
  "category": "水果",
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_per_serving": {
    "calories": 52,
    "protein": 0.3,
    "carbohydrates": 14,
    "fat": 0.2,
    "fiber": 2.4,
    "sugar": 10.4,
    "sodium": 1
  },
  "brand": "测试品牌",
  "barcode": "1234567890123",
  "image_url": "https://example.com/apple.jpg",
  "created_by": "user@example.com",
  "created_at": "2024-01-15T08:30:00"
}
```

**失败 (404)**:
```json
{
  "detail": "食物不存在"
}
```

---

### 14. 更新食物

**端点**: `PUT /api/food/{food_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 更新食物信息（仅创建者可更新）

#### 请求参数

所有字段可选，只需要传递需要更新的字段。

```json
{
  "name": "更新后的苹果",
  "category": "水果"
}
```

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "更新后的苹果",
  "category": "水果",
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_per_serving": {
    "calories": 52,
    "protein": 0.3,
    "carbohydrates": 14,
    "fat": 0.2
  }
}
```

**失败 (404)**:
```json
{
  "detail": "食物不存在或无权更新"
}
```

---

### 15. 删除食物

**端点**: `DELETE /api/food/{food_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除食物（仅创建者可删除）

#### 响应示例

**成功 (200)**:
```json
{
  "message": "食物删除成功"
}
```

**失败 (404)**:
```json
{
  "detail": "食物不存在或无权删除"
}
```

---

### 食物记录管理

### 16. 创建食物记录

**端点**: `POST /api/food/record`
**认证**: ✅ 需要 JWT Token
**说明**: 记录食物摄入

#### 请求参数

```json
{
  "food_id": "507f1f77bcf86cd799439011",
  "serving_amount": 1.5,
  "recorded_at": "2024-01-15T08:30:00",
  "meal_type": "早餐",
  "notes": "测试备注"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| food_id | string | ✅ | 食物ID（本地库ObjectId） |
| serving_amount | number | ✅ | 食用份量数（1.5表示1.5份） |
| recorded_at | string | ✅ | 摄入时间（ISO 8601格式） |
| meal_type | string | ❌ | 餐次类型（早餐、午餐、晚餐、加餐） |
| notes | string | ❌ | 备注 |

#### 响应示例

**成功 (201)**:
```json
{
  "id": "507f191e810c19729de860ea",
  "user_email": "user@example.com",
  "food_name": "苹果",
  "serving_amount": 1.5,
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_data": {
    "calories": 78,
    "protein": 0.45,
    "carbohydrates": 21,
    "fat": 0.3,
    "fiber": 3.6,
    "sugar": 15.6,
    "sodium": 1.5
  },
  "recorded_at": "2024-01-15T08:30:00",
  "meal_type": "早餐",
  "notes": "测试备注",
  "food_id": "507f1f77bcf86cd799439011",
  "created_at": "2024-01-15T08:35:00"
}
```

---

### 17. 获取食物记录列表

**端点**: `GET /api/food/record/list`
**认证**: ✅ 需要 JWT Token
**说明**: 获取食物记录列表

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| start_date | string | ❌ | - | 开始日期（YYYY-MM-DD） |
| end_date | string | ❌ | - | 结束日期（YYYY-MM-DD） |
| meal_type | string | ❌ | - | 餐次类型筛选 |
| limit | integer | ❌ | 100 | 返回数量限制（最大500） |
| offset | integer | ❌ | 0 | 偏移量（用于分页） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 25,
  "records": [
    {
      "id": "507f191e810c19729de860ea",
      "user_email": "user@example.com",
      "food_name": "苹果",
      "serving_amount": 1.5,
      "serving_size": 100,
      "serving_unit": "克",
      "nutrition_data": {
        "calories": 78,
        "protein": 0.45,
        "carbohydrates": 21,
        "fat": 0.3
      },
      "recorded_at": "2024-01-15T08:30:00",
      "meal_type": "早餐",
      "notes": "测试备注",
      "created_at": "2024-01-15T08:35:00"
    }
  ],
  "total_nutrition": {
    "calories": 1850,
    "protein": 65,
    "carbohydrates": 230,
    "fat": 50
  }
}
```

---

### 18. 获取每日营养摘要

**端点**: `GET /api/food/record/daily/{target_date}`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定日期的营养摘要

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| target_date | string | ✅ | 目标日期（YYYY-MM-DD）路径参数 |

#### 响应示例

**成功 (200)**:
```json
{
  "date": "2024-01-15",
  "total_calories": 1850,
  "total_protein": 65,
  "total_carbohydrates": 230,
  "total_fat": 50,
  "meal_count": 8,
  "records": [
    {
      "id": "507f191e810c19729de860ea",
      "food_name": "苹果",
      "serving_amount": 1.5,
      "nutrition_data": {
        "calories": 78,
        "protein": 0.45,
        "carbohydrates": 21,
        "fat": 0.3
      },
      "recorded_at": "2024-01-15T08:30:00",
      "meal_type": "早餐"
    }
  ]
}
```

---

### 19. 更新食物记录

**端点**: `PUT /api/food/record/{record_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 更新食物记录（仅创建者可更新）

#### 请求参数

所有字段可选：

```json
{
  "serving_amount": 2.0,
  "meal_type": "午餐",
  "notes": "更新后的备注"
}
```

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f191e810c19729de860ea",
  "food_name": "苹果",
  "serving_amount": 2.0,
  "nutrition_data": {
    "calories": 104,
    "protein": 0.6,
    "carbohydrates": 28,
    "fat": 0.4
  },
  "meal_type": "午餐",
  "notes": "更新后的备注"
}
```

---

### 20. 删除食物记录

**端点**: `DELETE /api/food/record/{record_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除食物记录（仅创建者可删除）

#### 响应示例

**成功 (200)**:
```json
{
  "message": "记录删除成功"
}
```

---

## 条形码扫描 API

### 32. 从图片识别条形码

**端点**: `POST /api/food/barcode/recognize`
**认证**: ✅ 需要 JWT Token
**说明**: 从上传的图片中识别条形码数字

#### 请求参数

Content-Type: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | ✅ | 图片文件（JPG, PNG, BMP等） |

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "barcode": "6920546800053",
  "barcode_type": "EAN13",
  "message": "成功识别到条形码"
}
```

**识别失败 (200)**:
```json
{
  "success": false,
  "barcode": null,
  "barcode_type": null,
  "message": "未识别到条形码，请确保图片清晰且包含完整的条形码"
}
```

**后续步骤**：前端获取到条形码后，可以调用 `GET /api/food/barcode/{barcode}` 查询食品信息。

---

### 33. 扫描条形码查询食品信息

**端点**: `GET /api/food/barcode/{barcode}`
**认证**: ✅ 需要 JWT Token
**说明**: 通过条形码查询食品信息

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| barcode | string | ✅ | 条形码（路径参数） |

#### 工作流程

1. 验证条形码格式
2. 查询本地数据库（优先使用已有数据）
3. 如果本地没有，调用外部API查询
4. 返回食品信息供用户确认份量

#### 响应示例

**成功 (200) - 找到食品**:
```json
{
  "found": true,
  "message": "从本地数据库找到食品信息",
  "food_data": {
    "id": "507f1f77bcf86cd799439011",
    "name": "可口可乐",
    "brand": "可口可乐",
    "category": "饮料",
    "serving_size": 100,
    "serving_unit": "毫升",
    "nutrition_per_serving": {
      "calories": 43,
      "protein": 0,
      "carbohydrates": 10.6,
      "fat": 0
    },
    "barcode": "6901939613702",
    "image_url": "https://...",
    "source": "local"
  }
}
```

**失败 (200) - 未找到**:
```json
{
  "found": false,
  "message": "未找到该商品信息，请手动录入",
  "food_data": null
}
```

**失败 (400) - 无效条形码**:
```json
{
  "detail": "无效的条形码格式"
}
```

---

## 食谱管理 API

### 食谱基础管理

### 21. 创建食谱

**端点**: `POST /api/recipe/`
**认证**: ✅ 需要 JWT Token
**说明**: 创建自定义食谱

#### 请求参数

```json
{
  "name": "营养早餐",
  "description": "营养均衡的早餐组合",
  "category": "早餐",
  "foods": [
    {
      "food_id": "507f1f77bcf86cd799439011",
      "food_name": "鸡蛋",
      "serving_amount": 2.0,
      "serving_size": 50,
      "serving_unit": "克",
      "nutrition": {
        "calories": 150,
        "protein": 13,
        "carbohydrates": 1,
        "fat": 10
      }
    }
  ],
  "tags": ["早餐", "健康", "简单"],
  "image_url": "https://example.com/breakfast.jpg",
  "prep_time": 15
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 食谱名称 |
| description | string | ❌ | 食谱描述 |
| category | string | ❌ | 分类 |
| foods | array | ✅ | 食物列表（至少1个） |
| foods[].food_id | string | ✅ | 食物ID |
| foods[].food_name | string | ✅ | 食物名称 |
| foods[].serving_amount | number | ✅ | 份量数 |
| foods[].serving_size | number | ✅ | 每份大小（克） |
| foods[].serving_unit | string | ✅ | 份量单位 |
| foods[].nutrition | object | ✅ | 营养数据 |
| tags | array | ❌ | 标签列表 |
| image_url | string | ❌ | 图片URL |
| prep_time | integer | ❌ | 准备时间（分钟） |

#### 响应示例

**成功 (201)**:
```json
{
  "id": "507f1f77bcf86cd799439012",
  "name": "营养早餐",
  "description": "营养均衡的早餐组合",
  "category": "早餐",
  "foods": [
    {
      "food_id": "507f1f77bcf86cd799439011",
      "food_name": "鸡蛋",
      "serving_amount": 2.0,
      "serving_size": 50,
      "serving_unit": "克",
      "nutrition": {
        "calories": 150,
        "protein": 13,
        "carbohydrates": 1,
        "fat": 10
      }
    }
  ],
  "total_nutrition": {
    "calories": 150,
    "protein": 13,
    "carbohydrates": 1,
    "fat": 10
  },
  "tags": ["早餐", "健康", "简单"],
  "image_url": "https://example.com/breakfast.jpg",
  "prep_time": 15,
  "created_by": "user@example.com",
  "created_at": "2024-01-15T08:30:00",
  "updated_at": "2024-01-15T08:30:00"
}
```

---

### 22. 搜索食谱

**端点**: `GET /api/recipe/search`
**认证**: ✅ 需要 JWT Token
**说明**: 搜索食谱

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | ❌ | - | 搜索关键词（搜索名称、描述） |
| category | string | ❌ | - | 分类筛选 |
| tags | array | ❌ | - | 标签筛选（可传递多个） |
| limit | integer | ❌ | 20 | 返回数量限制（最大100） |
| offset | integer | ❌ | 0 | 偏移量（用于分页） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 15,
  "recipes": [
    {
      "id": "507f1f77bcf86cd799439012",
      "name": "营养早餐",
      "description": "营养均衡的早餐组合",
      "category": "早餐",
      "foods": [...],
      "total_nutrition": {
        "calories": 150,
        "protein": 13,
        "carbohydrates": 1,
        "fat": 10
      },
      "tags": ["早餐", "健康"],
      "image_url": "https://...",
      "prep_time": 15,
      "created_at": "2024-01-15T08:30:00"
    }
  ]
}
```

---

### 23. 按名称搜索食谱ID

**端点**: `GET /api/recipe/search-id`
**认证**: ✅ 需要 JWT Token
**说明**: 通过食谱名称搜索ID（用于快速查找和自动完成）

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | ✅ | - | 搜索关键词 |
| limit | integer | ❌ | 10 | 返回数量限制（最大50） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 5,
  "recipes": [
    {
      "id": "507f1f77bcf86cd799439012",
      "name": "营养早餐",
      "category": "早餐"
    }
  ]
}
```

---

### 24. 获取食谱分类

**端点**: `GET /api/recipe/categories`
**认证**: ✅ 需要 JWT Token
**说明**: 获取所有食谱分类

#### 响应示例

**成功 (200)**:
```json
["早餐", "午餐", "晚餐", "零食", "汤品"]
```

---

### 25. 获取食谱详情

**端点**: `GET /api/recipe/{recipe_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定食谱的详细信息

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f1f77bcf86cd799439012",
  "name": "营养早餐",
  "description": "营养均衡的早餐组合",
  "category": "早餐",
  "foods": [
    {
      "food_id": "507f1f77bcf86cd799439011",
      "food_name": "鸡蛋",
      "serving_amount": 2.0,
      "nutrition": {
        "calories": 150,
        "protein": 13
      }
    }
  ],
  "total_nutrition": {
    "calories": 150,
    "protein": 13,
    "carbohydrates": 1,
    "fat": 10
  },
  "tags": ["早餐", "健康"],
  "image_url": "https://...",
  "prep_time": 15,
  "created_by": "user@example.com",
  "created_at": "2024-01-15T08:30:00",
  "updated_at": "2024-01-15T08:30:00"
}
```

---

### 26. 更新食谱

**端点**: `PUT /api/recipe/{recipe_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 更新食谱（仅创建者可更新）

#### 请求参数

所有字段可选：

```json
{
  "name": "更新后的食谱名称",
  "description": "更新后的描述"
}
```

#### 响应示例

**成功 (200)**:
```json
{
  "id": "507f1f77bcf86cd799439012",
  "name": "更新后的食谱名称",
  "description": "更新后的描述",
  "category": "早餐",
  "foods": [...],
  "total_nutrition": {...}
}
```

---

### 27. 删除食谱

**端点**: `DELETE /api/recipe/{recipe_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除食谱（仅创建者可删除）

#### 响应示例

**成功 (200)**:
```json
{
  "message": "食谱删除成功"
}
```

---

### 食谱记录管理

### 28. 创建食谱记录

**端点**: `POST /api/recipe/record`
**认证**: ✅ 需要 JWT Token
**说明**: 记录食谱摄入（为食谱中的每个食物创建记录）

#### 请求参数

```json
{
  "recipe_id": "507f1f77bcf86cd799439012",
  "scale": 1.0,
  "recorded_at": "2024-01-15T08:30:00",
  "meal_type": "早餐",
  "notes": "测试备注"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| recipe_id | string | ✅ | - | 食谱ID（本地库ObjectId） |
| scale | number | ❌ | 1.0 | 份量倍数（0.5表示半份，2.0表示2份） |
| recorded_at | string | ✅ | - | 摄入时间（ISO 8601格式） |
| meal_type | string | ❌ | - | 餐次类型（早餐、午餐、晚餐、加餐） |
| notes | string | ❌ | - | 备注 |

**说明**：系统会为食谱中的每个食物创建一条记录，并在备注中自动添加"[来自食谱: {食谱名称}]"标记。

#### 响应示例

**成功 (201)**:
```json
{
  "message": "食谱记录创建成功",
  "recipe_name": "营养早餐",
  "batch_id": "batch_507f1f77bcf86cd799439013",
  "total_records": 3,
  "record_ids": [
    "507f191e810c19729de860ea",
    "507f191e810c19729de860eb",
    "507f191e810c19729de860ec"
  ],
  "total_nutrition": {
    "calories": 450,
    "protein": 35,
    "carbohydrates": 50,
    "fat": 15
  }
}
```

---

### 29. 获取食谱记录列表

**端点**: `GET /api/recipe/record`
**认证**: ✅ 需要 JWT Token
**说明**: 获取食谱记录列表（批量查询）

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| start_date | string | ❌ | - | 开始日期（YYYY-MM-DD） |
| end_date | string | ❌ | - | 结束日期（YYYY-MM-DD） |
| meal_type | string | ❌ | - | 餐次类型筛选 |
| limit | integer | ❌ | 100 | 返回数量限制（最大500） |
| offset | integer | ❌ | 0 | 偏移量（用于分页） |

#### 响应示例

**成功 (200)**:
```json
{
  "total": 10,
  "batches": [
    {
      "batch_id": "batch_507f1f77bcf86cd799439013",
      "recipe_name": "营养早餐",
      "total_records": 3,
      "recorded_at": "2024-01-15T08:30:00",
      "meal_type": "早餐",
      "total_nutrition": {
        "calories": 450,
        "protein": 35,
        "carbohydrates": 50,
        "fat": 15
      },
      "notes": "测试备注"
    }
  ],
  "total_nutrition": {
    "calories": 2100,
    "protein": 120,
    "carbohydrates": 250,
    "fat": 70
  }
}
```

---

### 30. 更新食谱记录

**端点**: `PUT /api/recipe/record/{batch_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 更新食谱记录（批量更新该批次的所有食物记录）

#### 请求参数

所有字段可选：

```json
{
  "recorded_at": "2024-01-15T12:00:00",
  "meal_type": "午餐",
  "notes": "更新后的备注"
}
```

**注意**：不支持修改份量倍数，如需修改份量请删除后重新创建食谱记录。

#### 响应示例

**成功 (200)**:
```json
{
  "message": "食谱记录更新成功",
  "recipe_name": "营养早餐",
  "batch_id": "batch_507f1f77bcf86cd799439013",
  "updated_count": 3,
  "total_nutrition": {
    "calories": 450,
    "protein": 35,
    "carbohydrates": 50,
    "fat": 15
  }
}
```

---

### 31. 删除食谱记录

**端点**: `DELETE /api/recipe/record/{batch_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除食谱记录（删除该批次的所有食物记录）

#### 响应示例

**成功 (200)**:
```json
{
  "message": "食谱记录删除成功",
  "data": {
    "deleted_count": 3
  }
}
```

---

## 运动管理 API

### 运动类型管理

### 34. 创建自定义运动类型

**端点**: `POST /api/sports/create-sport`
**认证**: ✅ 需要 JWT Token
**说明**: 创建用户自定义的运动类型

#### 请求参数

```json
{
  "sport_type": "瑜伽",
  "describe": "低至中等强度运动，提高柔韧性和平衡能力",
  "METs": 3.0
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sport_type | string | ✅ | 运动类型名称 |
| describe | string | ✅ | 运动描述 |
| METs | float | ✅ | 代谢当量（必须大于0） |

**METs说明**：
- METs（Metabolic Equivalent of Task）表示运动强度
- METs = 1 表示静息代谢率
- 常见运动的METs值：散步(3.5)、跑步(8)、游泳(6)、骑自行车(7)

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "自定义运动类型已创建"
}
```

**失败 (400)**:
```json
{
  "detail": "该运动类型已存在"
}
```

---

### 35. 更新自定义运动类型

**端点**: `POST /api/sports/update-sport`
**认证**: ✅ 需要 JWT Token
**说明**: 更新用户自定义的运动类型信息

#### 请求参数

```json
{
  "sport_type": "瑜伽",
  "describe": "低至中等强度运动，提高柔韧性和平衡能力，适合减压",
  "METs": 3.5
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sport_type | string | ✅ | 要更新的运动类型名称 |
| describe | string | ❌ | 新的运动描述（可选） |
| METs | float | ❌ | 新的METs值（可选，必须大于0） |

**注意**：
- 只能更新用户自己创建的运动类型
- 不能更新系统默认运动类型
- 只需传递需要更新的字段

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "自定义运动类型已更新"
}
```

**失败 (404)**:
```json
{
  "detail": "自定义运动类型未找到"
}
```

---

### 36. 删除自定义运动类型

**端点**: `GET /api/sports/delete-sport/{sport_type}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除用户自定义的运动类型

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sport_type | string | ✅ | 要删除的运动类型名称 |

#### 请求示例

```bash
GET /api/sports/delete-sport/瑜伽
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "自定义运动类型已删除"
}
```

**失败 (404)**:
```json
{
  "detail": "删除自定义运动类型失败，运动类型不存在或无权删除"
}
```

---

### 37. 获取可用运动类型列表

**端点**: `GET /api/sports/get-available-sports-types`
**认证**: ✅ 需要 JWT Token
**说明**: 获取用户可用的运动类型列表，包括系统默认类型和用户自定义类型

#### 响应示例

**成功 (200)**:
```json
[
  {
    "sport_type": "跑步",
    "describe": "高强度有氧运动，有效提升心肺功能和燃烧卡路里，适合大多数健康成年人",
    "METs": 8.0
  },
  {
    "sport_type": "游泳",
    "describe": "低冲击性全身运动，锻炼几乎所有肌肉群，对关节友好，适合各年龄段人群",
    "METs": 6.0
  },
  {
    "sport_type": "骑自行车",
    "describe": "中等至高强度有氧运动，主要锻炼下肢肌肉，提升心肺耐力，可调节强度适应不同体能水平",
    "METs": 7.0
  },
  {
    "sport_type": "散步",
    "describe": "低强度有氧运动，适合初学者或恢复期人群，有助于改善心血管健康和日常活动能力",
    "METs": 3.5
  }
]
```

**系统默认运动类型**：
- 跑步 (METs: 8.0)
- 游泳 (METs: 6.0)
- 骑自行车 (METs: 7.0)
- 散步 (METs: 3.5)

---

### 运动记录管理

### 38. 记录运动

**端点**: `POST /api/sports/log-sports`
**认证**: ✅ 需要 JWT Token
**说明**: 记录一次运动活动，系统会自动根据METs、用户体重和运动时长计算消耗的卡路里

**前置条件**: 用户必须已完成身体数据收集（需要体重数据）

#### 请求参数

```json
{
  "sport_type": "跑步",
  "created_at": "2025-11-17T08:30:00",
  "duration_time": 30
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sport_type | string | ✅ | 运动类型（必须是已存在的运动类型） |
| created_at | datetime | ❌ | 运动开始时间（ISO 8601格式，默认当前时间） |
| duration_time | int | ✅ | 运动持续时间（分钟，必须大于0） |

**卡路里计算公式**：
```
消耗卡路里 = METs × 体重(kg) × 运动时长(小时)
```

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "运动记录已保存"
}
```

**失败 (404)**:
```json
{
  "detail": "运动类型未找到"
}
```

**失败 (404) - 缺少体重数据**:
```json
{
  "detail": "用户体重数据未找到"
}
```

---

### 39. 更新运动记录

**端点**: `POST /api/sports/update-sport-record`
**认证**: ✅ 需要 JWT Token
**说明**: 更新已有的运动记录

#### 请求参数

```json
{
  "_id": "507f1f77bcf86cd799439011",
  "sport_type": "游泳",
  "created_at": "2025-11-17T09:00:00",
  "duration_time": 45
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| _id | string | ✅ | 运动记录ID（从查询接口获取的record_id） |
| sport_type | string | ❌ | 新的运动类型（可选） |
| created_at | datetime | ❌ | 新的运动开始时间（可选） |
| duration_time | int | ❌ | 新的运动持续时间（可选，必须大于0） |

**注意**：
- 只能更新用户自己的运动记录
- 至少需要提供一个要更新的字段
- 如果更新了运动类型或持续时间，系统会自动重新计算消耗的卡路里

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "运动记录已更新"
}
```

**失败 (404)**:
```json
{
  "detail": "运动记录未找到"
}
```

---

### 40. 删除运动记录

**端点**: `GET /api/sports/delete-sport-record/{record_id}`
**认证**: ✅ 需要 JWT Token
**说明**: 删除指定的运动记录

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | string | ✅ | 运动记录ID |

#### 请求示例

```bash
GET /api/sports/delete-sport-record/507f1f77bcf86cd799439011
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 响应示例

**成功 (200)**:
```json
{
  "success": true,
  "message": "运动记录已删除"
}
```

**失败 (404)**:
```json
{
  "detail": "删除运动记录失败，记录不存在或无权删除"
}
```

---

### 41. 搜索运动记录

**端点**: `POST /api/sports/search-sports-records`
**认证**: ✅ 需要 JWT Token
**说明**: 根据日期范围和运动类型搜索运动记录

#### 请求参数

```json
{
  "start_date": "2025-11-01",
  "end_date": "2025-11-17",
  "sport_type": "跑步"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start_date | date | ❌ | 开始日期（格式：YYYY-MM-DD） |
| end_date | date | ❌ | 结束日期（格式：YYYY-MM-DD） |
| sport_type | string | ❌ | 运动类型 |

**注意**：
- 所有参数都是可选的
- 如果不提供任何参数，将返回用户所有运动记录
- end_date 必须大于或等于 start_date

#### 响应示例

**成功 (200)**:
```json
[
  {
    "record_id": "507f1f77bcf86cd799439011",
    "sport_type": "跑步",
    "created_at": "2025-11-17T08:30:00",
    "duration_time": 30,
    "calories_burned": 280.0
  },
  {
    "record_id": "507f1f77bcf86cd799439012",
    "sport_type": "跑步",
    "created_at": "2025-11-15T07:00:00",
    "duration_time": 45,
    "calories_burned": 420.0
  }
]
```

**失败 (400) - 日期范围错误**:
```json
{
  "detail": "end_date must be after start_date"
}
```

---

### 42. 获取全部运动记录

**端点**: `GET /api/sports/get-all-sports-records`
**认证**: ✅ 需要 JWT Token
**说明**: 获取用户的全部运动记录

#### 响应示例

**成功 (200)**:
```json
[
  {
    "record_id": "507f1f77bcf86cd799439011",
    "sport_type": "跑步",
    "created_at": "2025-11-17T08:30:00",
    "duration_time": 30,
    "calories_burned": 280.0
  },
  {
    "record_id": "507f1f77bcf86cd799439012",
    "sport_type": "游泳",
    "created_at": "2025-11-15T07:00:00",
    "duration_time": 60,
    "calories_burned": 420.0
  },
  {
    "record_id": "507f1f77bcf86cd799439013",
    "sport_type": "散步",
    "created_at": "2025-11-10T18:00:00",
    "duration_time": 20,
    "calories_burned": 81.67
  }
]
```

---

### 43. 获取运动报告

**端点**: `GET /api/sports/sports-report`
**认证**: ✅ 需要 JWT Token
**说明**: 获取用户最近7天的运动统计报告

#### 响应示例

**成功 (200)**:
```json
{
  "total_activities": 5,
  "total_duration": 180,
  "total_calories": 1260.5,
  "favorite_sport": "跑步",
  "sport_details": {
    "跑步": {
      "count": 3,
      "total_duration": 120,
      "total_calories": 1008.0,
      "avg_duration": 40.0,
      "avg_calories": 336.0
    },
    "游泳": {
      "count": 1,
      "total_duration": 30,
      "total_calories": 157.5,
      "avg_duration": 30.0,
      "avg_calories": 157.5
    },
    "散步": {
      "count": 1,
      "total_duration": 30,
      "total_calories": 95.0,
      "avg_duration": 30.0,
      "avg_calories": 95.0
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| total_activities | int | 总运动次数 |
| total_duration | int | 总运动时长（分钟） |
| total_calories | float | 总消耗卡路里 |
| favorite_sport | string | 最常进行的运动类型（按次数） |
| sport_details | object | 各运动类型的详细统计 |

**sport_details 字段说明**：
- `count`: 该运动类型的次数
- `total_duration`: 该运动类型的总时长（分钟）
- `total_calories`: 该运动类型的总消耗卡路里
- `avg_duration`: 该运动类型的平均时长（分钟）
- `avg_calories`: 该运动类型的平均消耗卡路里

**无运动记录时的响应 (200)**:
```json
{
  "total_activities": 0,
  "total_duration": 0,
  "total_calories": 0,
  "favorite_sport": null,
  "sport_details": {}
}
```

---

## 可视化报告 API

### 44. 获取每日卡路里摘要

**端点**: `GET /api/visualization/daily-calorie-summary`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定日期的卡路里摄入、消耗和预算摘要

**对应 Issue**: #22 - 可视化报告：每日卡路里摘要

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| target_date | date | ❌ | 今天 | 目标日期(YYYY-MM-DD) |

#### 响应示例

**成功 (200)**:
```json
{
  "date": "2025-11-23",
  "total_intake": 1850.5,
  "total_burned": 350.0,
  "daily_goal": 2000.0,
  "net_calories": 1500.5,
  "goal_percentage": 92.53,
  "is_over_budget": false
}
```

**字段说明**:
- `total_intake`: 当日总摄入卡路里
- `total_burned`: 当日运动总消耗卡路里
- `daily_goal`: 每日卡路里目标
- `net_calories`: 净卡路里(摄入 - 消耗)
- `goal_percentage`: 摄入占目标的百分比
- `is_over_budget`: 是否超出预算

---

### 45. 获取营养素与食物来源分析

**端点**: `GET /api/visualization/nutrition-analysis`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定日期范围内的营养素比例和食物类别分布分析

**对应 Issue**: #25 - 可视化报告：营养素与食物来源分析

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start_date | date | ✅ | 开始日期(YYYY-MM-DD) |
| end_date | date | ✅ | 结束日期(YYYY-MM-DD) |

#### 响应示例

**成功 (200)**:
```json
{
  "date_range": {
    "start_date": "2025-11-01",
    "end_date": "2025-11-23"
  },
  "macronutrient_ratio": {
    "protein": 25.5,
    "carbohydrates": 50.2,
    "fat": 24.3
  },
  "nutrition_vs_recommended": [
    {
      "nutrient_name": "蛋白质",
      "actual": 1850.0,
      "recommended": 1932.0,
      "percentage": 95.76
    },
    {
      "nutrient_name": "碳水化合物",
      "actual": 6200.0,
      "recommended": 6440.0,
      "percentage": 96.27
    },
    {
      "nutrient_name": "脂肪",
      "actual": 1610.0,
      "recommended": 1610.0,
      "percentage": 100.0
    }
  ],
  "food_category_distribution": [
    {
      "category": "主食",
      "count": 45,
      "total_calories": 5500.0,
      "percentage": 35.5
    },
    {
      "category": "蔬菜",
      "count": 38,
      "total_calories": 1200.0,
      "percentage": 7.7
    }
  ]
}
```

**字段说明**:
- `macronutrient_ratio`: 宏量营养素比例(蛋白质、碳水、脂肪)，单位：百分比
- `nutrition_vs_recommended`: 各营养素实际摄入量vs推荐量
- `food_category_distribution`: 不同食物类别的摄入分布

---

### 46. 获取时间序列趋势分析

**端点**: `GET /api/visualization/time-series-trend`
**认证**: ✅ 需要 JWT Token
**说明**: 获取指定日期范围内的卡路里和体重趋势数据

**对应 Issue**: #26 - 可视化报告：时间序列趋势分析

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| start_date | date | ✅ | - | 开始日期(YYYY-MM-DD) |
| end_date | date | ✅ | - | 结束日期(YYYY-MM-DD) |
| view_type | string | ❌ | day | 视图类型: day/week/month |

#### 响应示例

**成功 (200)**:
```json
{
  "view_type": "day",
  "date_range": {
    "start_date": "2025-11-01",
    "end_date": "2025-11-23"
  },
  "intake_trend": [
    {"date": "2025-11-01", "value": 1850.5},
    {"date": "2025-11-02", "value": 2100.0},
    {"date": "2025-11-03", "value": 1920.3}
  ],
  "burned_trend": [
    {"date": "2025-11-01", "value": 350.0},
    {"date": "2025-11-02", "value": 420.0},
    {"date": "2025-11-03", "value": 280.5}
  ],
  "weight_trend": [
    {"date": "2025-11-23", "value": 70.0}
  ]
}
```

**字段说明**:
- `view_type`: 数据聚合方式(day-每日/week-每周/month-每月)
- `intake_trend`: 卡路里摄入趋势数据
- `burned_trend`: 卡路里消耗趋势数据
- `weight_trend`: 体重变化趋势数据

---

### 47. 导出健康数据报告

**端点**: `GET /api/visualization/export-report`
**认证**: ✅ 需要 JWT Token
**说明**: 导出指定日期范围内的完整健康数据报告

**对应 Issue**: #23 - 可视化报告：报告导出

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start_date | date | ✅ | 开始日期(YYYY-MM-DD) |
| end_date | date | ✅ | 结束日期(YYYY-MM-DD) |

#### 响应示例

**成功 (200)**:
```json
{
  "user_info": {
    "username": "张三",
    "email": "user@example.com",
    "age": 27,
    "gender": "male",
    "height": 175.0,
    "weight": 70.0,
    "health_goal_type": "lose_weight",
    "daily_calorie_goal": 2105.16
  },
  "date_range": {
    "start_date": "2025-11-01",
    "end_date": "2025-11-23"
  },
  "summary": {
    "days_count": 23,
    "total_food_records": 156,
    "total_sports_records": 18,
    "total_intake_calories": 42560.5,
    "total_burned_calories": 6340.0,
    "average_daily_intake": 1850.89,
    "average_daily_burned": 275.65
  },
  "daily_calorie_summary": {
    "date": "2025-11-23",
    "total_intake": 1850.5,
    "total_burned": 350.0,
    "daily_goal": 2105.16,
    "net_calories": 1500.5,
    "goal_percentage": 87.9,
    "is_over_budget": false
  },
  "nutrition_analysis": {
    "date_range": {...},
    "macronutrient_ratio": {...},
    "nutrition_vs_recommended": [...],
    "food_category_distribution": [...]
  },
  "time_series_trend": {
    "view_type": "day",
    "date_range": {...},
    "intake_trend": [...],
    "burned_trend": [...],
    "weight_trend": [...]
  },
  "generated_at": "2025-11-23T15:30:00"
}
```

**字段说明**:
- `user_info`: 用户基本信息
- `summary`: 总体统计摘要
- `daily_calorie_summary`: 最新日期的每日卡路里摘要
- `nutrition_analysis`: 营养素分析数据
- `time_series_trend`: 时间序列趋势数据
- `generated_at`: 报告生成时间

**使用说明**:
- 前端可以使用返回的数据生成PDF报告或长图
- 建议使用图表库(如 ECharts、Chart.js)可视化趋势数据
- 可以添加用户头像、个性化标题等元素

---

## 数据模型

### 用户相关

#### Gender（性别）

```typescript
type Gender = "male" | "female";
```

#### ActivityLevel（活动水平）

```typescript
type ActivityLevel =
  | "sedentary"           // 久坐
  | "lightly_active"      // 轻度活动
  | "moderately_active"   // 中度活动
  | "very_active"         // 重度活动
  | "extremely_active";   // 极重度活动
```

#### HealthGoalType（健康目标类型）

```typescript
type HealthGoalType =
  | "lose_weight"      // 减重
  | "gain_weight"      // 增重
  | "maintain_weight"; // 保持体重
```

#### UserProfile（用户资料）

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

#### WeightRecord（体重记录）

```typescript
interface WeightRecord {
  id: string;              // 记录ID（MongoDB ObjectId）
  user_email: string;      // 用户邮箱
  weight: number;          // 体重（公斤，0-500）
  recorded_at: string;     // 记录时间（ISO 8601格式）
  notes?: string;          // 备注（最多200字符）
  created_at: string;      // 创建时间（ISO 8601格式）
}
```

**使用场景**：
- 记录用户的历史体重数据
- 支持体重趋势分析和可视化
- 在可视化报告中展示体重变化曲线
- 按日期范围查询和统计

**注意**：
- `recorded_at` 是用户实际测量体重的时间
- `created_at` 是系统记录的创建时间
- 记录按 `recorded_at` 时间倒序排列
- 只能查看、修改、删除自己的体重记录

---

### 食物相关

#### NutritionData（营养数据）

```typescript
interface NutritionData {
  calories: number;         // 卡路里（千卡）- 必填
  protein: number;          // 蛋白质（克）- 必填
  carbohydrates: number;    // 碳水化合物（克）- 必填
  fat: number;              // 脂肪（克）- 必填
  fiber?: number;           // 膳食纤维（克）- 可选
  sugar?: number;           // 糖分（克）- 可选
  sodium?: number;          // 钠（毫克）- 可选
}
```

#### Food（食物）

```typescript
interface Food {
  id: string;                           // 食物ID（MongoDB ObjectId）
  name: string;                         // 食物名称
  category?: string;                    // 分类（水果、蔬菜、肉类等）
  serving_size: number;                 // 标准份量（克）
  serving_unit: string;                 // 份量单位（默认：克）
  nutrition_per_serving: NutritionData; // 每份营养数据
  full_nutrition?: object;              // 完整营养信息（可选）
  brand?: string;                       // 品牌
  barcode?: string;                     // 条形码
  image_url?: string;                   // 图片URL
  source?: string;                      // 数据来源（local/boohee）
  created_by: string;                   // 创建者（邮箱或"all"）
  created_at: string;                   // 创建时间
  boohee_id?: string;                   // 薄荷健康ID
  boohee_code?: string;                 // 薄荷健康代码
}
```

**数据来源说明**：
- `local`: 用户手动创建
- `boohee`: 从薄荷健康API获取并缓存
- `created_by="all"`: 所有用户可见（系统食物、薄荷缓存）
- `created_by=邮箱`: 仅创建者可见（用户私有食物）

#### FoodRecord（食物记录）

```typescript
interface FoodRecord {
  id: string;                    // 记录ID
  user_email: string;            // 用户邮箱
  food_id?: string;              // 食物ID（如果存在）
  food_name: string;             // 食物名称（快照）
  serving_amount: number;        // 食用份量数
  serving_size: number;          // 每份大小（克）
  serving_unit: string;          // 份量单位
  nutrition_data: NutritionData; // 营养数据（快照，已乘以份量）
  full_nutrition?: object;       // 完整营养信息（可选）
  recorded_at: string;           // 摄入时间（用户实际食用时间）
  meal_type?: string;            // 餐次类型（早餐、午餐、晚餐、加餐）
  notes?: string;                // 备注
  batch_id?: string;             // 批次ID（食谱记录专用）
  created_at: string;            // 记录时间（系统创建时间）
}
```

**重要说明**：
- `recorded_at`: 用户实际食用的时间
- `created_at`: 系统记录的时间
- `nutrition_data`: 已经乘以 `serving_amount`，是实际摄入的营养
- 数据快照：记录时保存食物信息，后续修改食物不影响历史记录

#### MealType（餐次类型）

```typescript
type MealType = "早餐" | "午餐" | "晚餐" | "加餐";
```

---

### 食谱相关

#### RecipeFoodItem（食谱中的食物项）

```typescript
interface RecipeFoodItem {
  food_id: string;               // 食物ID
  food_name: string;             // 食物名称
  serving_amount: number;        // 份量数
  serving_size: number;          // 每份大小（克）
  serving_unit: string;          // 份量单位
  nutrition: NutritionData;      // 营养数据
}
```

#### Recipe（食谱）

```typescript
interface Recipe {
  id: string;                     // 食谱ID
  name: string;                   // 食谱名称
  description?: string;           // 描述
  category?: string;              // 分类（早餐、午餐、晚餐等）
  foods: RecipeFoodItem[];        // 食物列表
  total_nutrition: NutritionData; // 总营养（自动计算）
  total_full_nutrition?: object;  // 完整营养信息（可选）
  tags?: string[];                // 标签
  image_url?: string;             // 图片URL
  prep_time?: number;             // 准备时间（分钟）
  created_by: string;             // 创建者（邮箱或"all"）
  created_at: string;             // 创建时间
  updated_at: string;             // 更新时间
}
```

**食谱特点**：
- 食谱是多个食物的组合
- `total_nutrition` 由系统自动计算（食物营养总和）
- 支持标签和分类筛选
- 权限控制同食物（系统食谱vs用户食谱）

#### RecipeRecordBatch（食谱记录批次）

```typescript
interface RecipeRecordBatch {
  batch_id: string;              // 批次ID（格式：batch_XXXXXXXXX）
  recipe_id: string;             // 食谱ID
  recipe_name: string;           // 食谱名称
  scale: number;                 // 份量倍数（0.5=半份，1.0=一份，2.0=两份）
  total_records: number;         // 创建的食物记录数量
  record_ids: string[];          // 食物记录ID列表
  total_nutrition: NutritionData;// 总营养（已乘以scale）
  recorded_at: string;           // 摄入时间
  meal_type?: string;            // 餐次类型
  notes?: string;                // 备注
  created_at: string;            // 创建时间
}
```

**批次管理说明**：
- 一次食谱记录创建一个批次（batch）
- 批次包含多条食物记录（每个食物一条）
- 通过 `batch_id` 统一管理
- 更新/删除批次时，所有相关食物记录同步操作
- 食物记录的备注自动添加 `[来自食谱: 食谱名称]` 标记

---

### 条形码相关

#### BarcodeRecognitionResult（条形码识别结果）

```typescript
interface BarcodeRecognitionResult {
  success: boolean;          // 是否识别成功
  barcode?: string;          // 条形码数字
  barcode_type?: string;     // 条形码类型（EAN13, EAN8, UPC-A等）
  message: string;           // 响应消息
}
```

#### BarcodeScanResult（条形码扫描结果）

```typescript
interface BarcodeScanResult {
  found: boolean;            // 是否找到食品信息
  message: string;           // 响应消息
  food_data?: {              // 食品数据（找到时返回）
    id?: string;             // 本地食物ID（如果已缓存）
    name: string;            // 食品名称
    brand?: string;          // 品牌
    category?: string;       // 分类
    serving_size: number;    // 标准份量
    serving_unit: string;    // 份量单位
    nutrition_per_serving: NutritionData;  // 营养数据
    full_nutrition?: object; // 完整营养信息
    barcode: string;         // 条形码
    image_url?: string;      // 图片URL
    source: string;          // 来源（local/external）
  };
}
```

**条形码查询流程**：
```
1. 上传图片 → 识别条形码 → 获取条形码数字
2. 查询本地数据库（已缓存的食品）
3. 本地未找到 → 调用外部API查询
4. 找到 → 返回食品信息 → 用户确认份量 → 创建记录
5. 未找到 → 提示手动录入
```

---

### 运动相关

#### SportsType（运动类型）

```typescript
interface SportsType {
  sport_type: string;     // 运动类型名称
  describe: string;       // 运动描述
  METs: number;          // 代谢当量（必须大于0）
  email: string;         // 创建者邮箱（"sport@default.com"表示系统默认）
}
```

**系统默认运动类型**：
- 跑步 (METs: 8.0)
- 游泳 (METs: 6.0)
- 骑自行车 (METs: 7.0)
- 散步 (METs: 3.5)

#### SportsRecord（运动记录）

```typescript
interface SportsRecord {
  record_id: string;        // 运动记录ID（MongoDB ObjectId）
  email: string;            // 用户邮箱
  sport_type: string;       // 运动类型
  created_at: string;       // 运动开始时间（ISO 8601格式）
  duration_time: number;    // 运动持续时间（分钟）
  calories_burned: number;  // 消耗的卡路里（自动计算）
}
```

**卡路里计算**：
```
消耗卡路里 = METs × 体重(kg) × 运动时长(小时)
```

**示例**（用户体重70kg，跑步30分钟）：
```
卡路里 = 8.0 × 70 × 0.5 = 280 卡路里
```

#### SportsReport（运动报告）

```typescript
interface SportDetails {
  count: number;            // 运动次数
  total_duration: number;   // 总时长（分钟）
  total_calories: number;   // 总消耗卡路里
  avg_duration: number;     // 平均时长（分钟）
  avg_calories: number;     // 平均消耗卡路里
}

interface SportsReport {
  total_activities: number;           // 总运动次数
  total_duration: number;             // 总运动时长（分钟）
  total_calories: number;             // 总消耗卡路里
  favorite_sport: string | null;      // 最常进行的运动类型（按次数）
  sport_details: Record<string, SportDetails>;  // 各运动类型的详细统计
}
```

**报告统计周期**：最近7天（不包括当天）

**METs（代谢当量）参考表**：

| 运动类型 | METs | 强度 |
|---------|------|------|
| 睡觉 | 0.9 | 静息 |
| 看电视 | 1.0 | 静息 |
| 办公工作 | 1.5 | 低 |
| 散步（慢速） | 2.0 | 低 |
| 瑜伽 | 2.5-4.0 | 低-中 |
| 散步（快速） | 3.5 | 中 |
| 力量训练（轻度） | 3.5 | 中 |
| 骑自行车（休闲） | 4.0 | 中 |
| 羽毛球 | 5.5 | 中-高 |
| 游泳（中速） | 6.0 | 高 |
| 骑自行车（中速） | 7.0 | 高 |
| 跑步（8km/h） | 8.0 | 高 |
| 篮球 | 8.0 | 高 |
| 跳绳 | 10.0 | 高 |
| 跑步（12km/h） | 12.5 | 很高 |

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
| 403 | 禁止访问 | 无权访问该资源 |
| 404 | 资源不存在 | 食物/食谱/记录不存在 |
| 409 | 冲突 | 邮箱已被注册、条形码已存在 |
| 422 | 验证错误 | 参数类型/范围错误 |
| 500 | 服务器内部错误 | 服务器异常 |

### 常见错误示例

#### 参数验证错误 (422)

```json
{
  "detail": [
    {
      "loc": ["body", "serving_amount"],
      "msg": "份量必须大于0",
      "type": "value_error"
    }
  ]
}
```

#### 认证失败 (401)

```json
{
  "detail": "无效的认证凭证"
}
```

#### 权限不足 (403)

```json
{
  "detail": "无权访问此食谱"
}
```

#### 资源不存在 (404)

```json
{
  "detail": "食物不存在"
}
```

#### 业务逻辑错误 (400)

```json
{
  "detail": "请先完成身体基本数据收集"
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

---

### 食物记录流程

#### 方式1：单个食物记录

```mermaid
graph LR
    A[搜索食物] --> B[选择食物]
    B --> C[确认份量]
    C --> D[创建记录]
```

**适用场景**：零食、加餐、单个食物

#### 方式2：食谱记录（推荐）

```mermaid
graph LR
    A[搜索食谱] --> B[选择食谱]
    B --> C[调整份量]
    C --> D[创建批次记录]
    D --> E[自动创建多条食物记录]
```

**适用场景**：正餐、常吃组合、快速记录

**优势**：
- 一键记录多个食物
- 自动计算总营养
- 批次管理，便于修改/删除
- 自动标记来源，可追溯

---

### 条形码扫描流程

```mermaid
graph TD
    A[拍摄/上传条形码图片] --> B[图像识别]
    B --> C{识别成功?}
    C -->|是| D[获取条形码数字]
    C -->|否| E[手动输入条形码]
    D --> F[查询本地数据库]
    E --> F
    F --> G{找到?}
    G -->|是| H[返回食品信息]
    G -->|否| I[调用外部API]
    I --> J{找到?}
    J -->|是| K[缓存到本地并返回]
    J -->|否| L[提示手动录入]
    H --> M[用户确认份量]
    K --> M
    M --> N[创建食物记录]
```

**关键步骤**：
1. 图片识别条形码（`POST /api/food/barcode/recognize`）
2. 查询食品信息（`GET /api/food/barcode/{barcode}`）
3. 确认份量
4. 创建记录（`POST /api/food/record`）

---

### 密码重置流程

```mermaid
graph TD
    A[1. 发送验证码] --> B[2. 用户查收邮件]
    B --> C[3. 输入验证码和新密码]
    C --> D[4. 密码重置成功]
    D --> E[5. 使用新密码登录]
```

---

### 运动记录流程

```mermaid
graph TD
    A[1. 用户登录] --> B[2. 完成身体数据收集]
    B --> C[3. 查看可用运动类型]
    C --> D{需要自定义运动?}
    D -->|是| E[4a. 创建自定义运动类型]
    D -->|否| F[4b. 直接记录运动]
    E --> F
    F --> G[5. 查看运动记录]
    G --> H[6. 查看运动报告]
```

#### 详细步骤

1. **用户登录** (`POST /api/auth/login`)
   - 获取 JWT Token
   - 用于后续所有运动相关请求

2. **完成身体数据收集** (`POST /api/user/body-data`)
   - 提供体重等身体数据
   - 运动记录需要体重数据来计算卡路里消耗

3. **查看可用运动类型** (`GET /api/sports/get-available-sports-types`)
   - 查看系统默认运动类型（跑步、游泳、骑自行车、散步）
   - 查看自己创建的自定义运动类型

4. **（可选）创建自定义运动类型** (`POST /api/sports/create-sport`)
   - 如果系统默认类型不满足需求
   - 创建符合自己需要的运动类型
   - 提供运动名称、描述和METs值

5. **记录运动** (`POST /api/sports/log-sports`)
   - 选择运动类型
   - 输入运动时长（分钟）
   - 系统自动根据公式计算卡路里消耗：`METs × 体重 × 时长(小时)`

6. **管理运动记录**
   - **查看记录**：`POST /api/sports/search-sports-records`（可按日期、运动类型筛选）
   - **更新记录**：`POST /api/sports/update-sport-record`（修改时间、类型、时长）
   - **删除记录**：`GET /api/sports/delete-sport-record/{record_id}`

7. **查看运动报告** (`GET /api/sports/sports-report`)
   - 查看最近7天的运动统计
   - 包括总次数、总时长、总卡路里
   - 了解最常进行的运动类型
   - 查看各运动类型的详细数据

**前端集成建议**：
- 在记录运动时实时显示卡路里预估
- 使用图表可视化运动数据（饼图、柱状图、折线图）
- 缓存运动类型列表减少请求
- 提供友好的中文错误提示
