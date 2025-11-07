# 食物/食谱库功能说明与API使用指南

## 概述

食物/食谱库模块提供了完整的食物信息管理、食谱管理和食物记录功能，包括：
- **食物管理**：创建、搜索、查询、删除食物信息
- **食谱管理**：创建、搜索、更新、删除食谱
- **食物记录**：记录每日食物摄入（支持单个和批量记录）、查询历史记录、营养统计
- **批量记录**：一次记录多个食物或通过食谱快速记录

---

## 数据结构

### 营养数据 (NutritionData)
```json
{
  "calories": 52.0,           // 卡路里（千卡）- 必填
  "protein": 0.3,             // 蛋白质（克）- 必填
  "carbohydrates": 14.0,      // 碳水化合物（克）- 必填
  "fat": 0.2,                 // 脂肪（克）- 必填
  "fiber": 2.4,               // 膳食纤维（克）- 可选
  "sugar": 10.0,              // 糖分（克）- 可选
  "sodium": 1.0               // 钠（毫克）- 可选
}
```

### 食物信息 (Food)
```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "苹果",
  "category": "水果",
  "serving_size": 100,        // 标准份量（克）
  "serving_unit": "克",
  "nutrition_per_serving": {  // 每份营养数据
    "calories": 52,
    "protein": 0.3,
    "carbohydrates": 14,
    "fat": 0.2,
    "fiber": 2.4,
    "sugar": 10,
    "sodium": 1
  },
  "full_nutrition": {...},    // 可选，完整营养信息
  "brand": null,              // 品牌（可选）
  "barcode": null,            // 条形码（可选）
  "image_url": null,          // 图片URL（可选）
  "source": "local",         // 数据来源：local / boohee
  "boohee_id": null,          // 薄荷健康ID（来源为boohee时存在）
  "boohee_code": null,        // 薄荷健康编码（来源为boohee时存在）
  "created_by": "user@example.com",
  "created_at": "2025-11-03T10:00:00"
}
```

### 食物记录 (FoodRecord)
```json
{
  "id": "507f1f77bcf86cd799439012",
  "user_email": "user@example.com",
  "food_name": "苹果",
  "serving_amount": 1.5,      // 食用份量数
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_data": {         // 实际摄入的营养数据
    "calories": 78,           // 1.5份的卡路里
    "protein": 0.45,
    "carbohydrates": 21,
    "fat": 0.3
  },
  "recorded_at": "2025-11-03T12:30:00",  // 摄入时间
  "meal_type": "加餐",        // 早餐/午餐/晚餐/加餐
  "notes": "下午茶",          // 备注
  "food_id": "507f1f77bcf86cd799439011", // 本地食物库 ID（薄荷食品会在搜索时自动落地）
  "created_at": "2025-11-03T12:30:00"   // 记录时间
}
```

### 食谱信息 (Recipe)
```json
{
  "id": "507f1f77bcf86cd799439013",
  "name": "健康早餐",
  "description": "营养均衡的早餐套餐",
  "category": "早餐",
  "foods": [
    {
      "food_id": "...",
      "food_name": "鸡蛋",
      "serving_amount": 2,
      "serving_size": 50,
      "serving_unit": "克",
      "nutrition": {...}
    }
  ],
  "total_nutrition": {...},  // 自动计算
  "tags": ["健康", "快手"],
  "prep_time": 10,
  "created_by": null,         // 系统食谱为null
  "created_at": "...",
  "updated_at": "..."
}
```

---

## 功能说明

### 一、食物管理

#### 核心特性
- ✅ 系统食物和用户自定义食物
- ✅ 系统食物所有用户可见
- ✅ 用户创建的食物仅创建者可见（私有）
- ✅ 支持按名称、品牌搜索
- ✅ 薄荷健康搜索结果自动落地到本地库（source = "boohee"）
- ✅ 通过本地ID统一访问食物详情
- ✅ 分页支持
- ✅ 营养数据快照（历史数据不受食物信息修改影响）

#### 权限控制

| 操作 | 系统食物 | 自己的食物 | 他人食物 |
|------|---------|-----------|---------|
| 查看 | ✓ | ✓ | ✗ |
| 搜索 | ✓ | ✓ | ✗ |
| 编辑 | ✗ | ✓ | ✗ |
| 删除 | ✗ | ✓ | ✗ |

**说明**：
- 系统食物（`created_by = null`）：所有用户可见
- 用户创建的食物（`created_by = 用户邮箱`）：只有创建者自己可见，其他用户无法查看、搜索或使用
- 薄荷健康搜索结果会自动以 `source = "boohee"` 的形式同步到本地库，供后续统一管理

### 二、食谱管理

#### 核心概念
**食谱**是一组食物的组合，代表一顿完整的餐食。每个食谱包含：
- 食谱基本信息（名称、描述、分类）
- 食物列表（每个食物包含份量和营养信息）
- 总营养数据（自动计算）
- 标签、准备时间等可选信息

#### 食谱类型
1. **系统食谱**（`created_by = null`）
   - 由系统预置，对所有用户可见
   - 用户无法编辑或删除
   - 通过 `init_recipe_data.py` 脚本初始化

2. **用户食谱**（`created_by = 用户邮箱`）
   - 用户自行创建
   - 仅创建者可见（私有）
   - 创建者拥有完全的编辑和删除权限

#### 权限控制

| 操作 | 系统食谱 | 自己的食谱 | 他人食谱 |
|------|---------|-----------|---------|
| 查看 | ✓ | ✓ | ✗ |
| 搜索 | ✓ | ✓ | ✗ |
| 编辑 | ✗ | ✓ | ✗ |
| 删除 | ✗ | ✓ | ✗ |
| 使用记录 | ✓ | ✓ | ✗ |

**说明**：
- 系统食谱（`created_by = null`）：所有用户可见
- 用户创建的食谱（`created_by = 用户邮箱`）：只有创建者自己可见，其他用户无法查看、搜索或使用

#### 搜索功能
支持以下搜索条件：
- **keyword**：在名称和描述中搜索
- **category**：按分类筛选
- **tags**：按标签筛选（支持多个）
- **limit/offset**：分页

**搜索结果包含**：
- 所有系统食谱（`created_by = null`）
- 自己创建的所有食谱（无论公开或私有）
- **不包含**其他用户创建的食谱

#### 初始化数据
系统提供 6 个默认食谱：
1. **健康早餐套餐** - 鸡蛋、牛奶、全麦面包
2. **减脂午餐** - 鸡胸肉、白米饭、西兰花
3. **增肌晚餐** - 牛肉、土豆、胡萝卜
4. **快手营养早餐** - 燕麦片、香蕉、酸奶
5. **轻食沙拉** - 生菜、番茄、虾仁、橄榄油
6. **能量午餐盒** - 三文鱼、红薯、菠菜

运行初始化脚本：
```bash
cd backend
python -m app.init_db.init_recipe_data
```

### 三、食物记录

#### 核心特性
- ✅ 记录具体摄入量
- ✅ 支持餐次分类（早/午/晚/加餐）
- ✅ 营养数据快照（历史数据不受食物信息修改影响）
- ✅ 支持按日期范围查询
- ✅ 自动计算营养总和
- ✅ 支持单个记录和批量记录
- ✅ 支持通过食谱快速记录

#### 批量记录功能
传统方式一次只能记录一个食物，新方式支持：
1. **批量记录多个食物**
2. **通过食谱快速记录**
3. **混合记录**（食物 + 食谱）

**记录逻辑**：
- 处理食物列表：为 `foods` 数组中的每个食物创建一条记录
- 处理食谱列表：为 `recipes` 数组中每个食谱的每个食物创建记录，备注自动添加 `[来自食谱: {食谱名称}]`
- 营养计算：汇总所有食物的营养数据，返回 `total_nutrition` 供前端显示

---

## API 端点

### 食物管理

#### 1. 创建食物
```http
POST /api/food/
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "鸡胸肉",
  "category": "肉类",
  "serving_size": 100,
  "serving_unit": "克",
  "nutrition_per_serving": {
    "calories": 165,
    "protein": 31,
    "carbohydrates": 0,
    "fat": 3.6,
    "fiber": 0,
    "sodium": 74
  },
  "brand": "某某品牌"
}
```

#### 2. 搜索食物
```http
GET /api/food/search?keyword=鸡&page=1&include_full_nutrition=true
Authorization: Bearer {access_token}
```

**查询参数**：
- `keyword` (可选): 搜索关键词（名称或品牌，支持模糊匹配）
- `page` (可选): 页码，默认 1，最大 10
- `include_full_nutrition` (可选): 是否同步拉取完整营养信息（默认 `true`）

**说明**：
- 请求会优先返回本地食物；若提供了关键词，也会调用薄荷健康搜索。
- 薄荷健康返回的数据会自动写入本地数据库（`source = "boohee"`），避免重复搜索。
- 响应中的每个条目都包含 `source`、`boohee_id`、`boohee_code`（若存在），使用返回的 `food_id`/`code` 作为后续操作的本地ID。

#### 3. 获取食物分类
```http
GET /api/food/categories
Authorization: Bearer {access_token}
```

#### 4. 获取食物详情
```http
GET /api/food/{food_id}
Authorization: Bearer {access_token}
```

**说明**：
- 仅支持查询本地食物库的 ID（薄荷搜索结果在首次检索时已自动落地，可直接使用返回的本地 ID）。
- 响应中包含 `source`、`boohee_id`、`boohee_code` 等来源信息，便于区分薄荷数据。

#### 5. 更新食物
```http
PUT /api/food/{food_id}
Authorization: Bearer {access_token}
```

#### 6. 删除食物
```http
DELETE /api/food/{food_id}
Authorization: Bearer {access_token}
```

### 食谱管理

#### 1. 创建食谱
```http
POST /api/recipe/
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "我的健康早餐",
  "description": "个人定制的早餐搭配",
  "category": "早餐",
  "foods": [
    {
      "food_id": "...",
      "food_name": "鸡蛋",
      "serving_amount": 2,
      "serving_size": 50,
      "serving_unit": "克",
      "nutrition": {
        "calories": 148,
        "protein": 12.6,
        "carbohydrates": 1.2,
        "fat": 10
      }
    }
  ],
  "tags": ["健康", "自制"],
  "prep_time": 10
}
```

#### 2. 搜索食谱
```http
GET /api/recipe/search?keyword=健康&category=早餐&tags=快手&limit=20&offset=0
Authorization: Bearer {access_token}
```

**查询参数**：
- `keyword` (可选): 搜索关键词
- `category` (可选): 分类筛选
- `tags` (可选): 标签筛选（支持多个）
- `limit` (可选): 返回数量（默认20）
- `offset` (可选): 偏移量

#### 3. 获取食谱详情
```http
GET /api/recipe/{recipe_id}
Authorization: Bearer {access_token}
```

#### 4. 获取食谱分类列表
```http
GET /api/recipe/categories
Authorization: Bearer {access_token}
```

#### 5. 更新食谱
```http
PUT /api/recipe/{recipe_id}
Authorization: Bearer {access_token}
```

#### 6. 删除食谱
```http
DELETE /api/recipe/{recipe_id}
Authorization: Bearer {access_token}
```

### 食物记录

#### 1. 单个食物记录
```http
POST /api/food/record
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "food_id": "507f1f77bcf86cd799439011",
  "serving_amount": 1.5,
  "recorded_at": "2025-11-03T12:30:00",
  "meal_type": "午餐",
  "notes": "水煮",
  "source": "auto"
}
```

**重要说明**：
- `food_id` 必须是本地食物库的 ID（薄荷搜索结果已自动落地，可直接使用返回的 `code`/`food_id`）
- `serving_amount`: 食用份量数（如 1.5 表示 1.5 份）
- 后端会根据食物库中的标准份量与营养数据自动计算实际摄入的营养信息
- `recorded_at`: 摄入时间（必填）
- `created_at`: 系统自动生成（记录时间）

#### 2. 批量记录（推荐）
```http
POST /api/food/record/batch
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "recorded_at": "2025-11-03T12:30:00",
  "meal_type": "午餐",
  "notes": "今天的午餐",
  "foods": [
    {
      "food_id": "...",
      "food_name": "白米饭",
      "serving_amount": 1,
      "serving_size": 100,
      "serving_unit": "克",
      "nutrition_data": {...}
    }
  ],
  "recipes": [
    {
      "recipe_id": "...",
      "recipe_name": "减脂午餐",
      "scale": 1.0,
      "foods": [...]  // 从食谱详情获取，根据scale调整
    }
  ]
}
```

**响应**：
```json
{
  "message": "批量记录成功",
  "total_records": 5,
  "total_nutrition": {...},
  "record_ids": ["...", "..."]
}
```

#### 3. 查询食物记录
```http
GET /api/food/record/list?start_date=2025-11-01&end_date=2025-11-03&meal_type=午餐
Authorization: Bearer {access_token}
```

**查询参数**：
- `start_date` (可选): 开始日期（YYYY-MM-DD）
- `end_date` (可选): 结束日期（YYYY-MM-DD）
- `meal_type` (可选): 餐次类型筛选
- `limit` (可选): 返回数量（默认100，最大500）
- `offset` (可选): 偏移量

#### 4. 获取每日营养摘要
```http
GET /api/food/record/daily-summary?date=2025-11-03
Authorization: Bearer {access_token}
```

#### 5. 更新食物记录
```http
PUT /api/food/record/{record_id}
Authorization: Bearer {access_token}
```

#### 6. 删除食物记录
```http
DELETE /api/food/record/{record_id}
Authorization: Bearer {access_token}
```

---

## 使用场景示例

### 场景1：用户记录早餐（单个记录）
1. 搜索食物：`GET /api/food/search?keyword=牛奶`
2. 选择食物并记录：
```json
POST /api/food/record
{
  "food_id": "搜索结果返回的本地ID",
  "serving_amount": 2,
  "recorded_at": "2025-11-03T08:00:00",
  "meal_type": "早餐",
  "notes": "纯牛奶",
  "source": "auto"
}
```

### 场景2：批量记录多个食物
用户午餐吃了米饭、鸡胸肉、西兰花：
```json
POST /api/food/record/batch
{
  "recorded_at": "2025-11-03T12:30:00",
  "meal_type": "午餐",
  "foods": [
    {"food_name": "白米饭", ...},
    {"food_name": "鸡胸肉", ...},
    {"food_name": "西兰花", ...}
  ]
}
```

### 场景3：通过食谱记录
用户按"健康早餐套餐"食谱准备早餐：
```json
POST /api/food/record/batch
{
  "recorded_at": "2025-11-03T08:00:00",
  "meal_type": "早餐",
  "recipes": [
    {
      "recipe_id": "...",
      "recipe_name": "健康早餐套餐",
      "scale": 1.0,
      "foods": [...]  // 从食谱详情获取
    }
  ]
}
```

### 场景4：混合记录（食物+食谱）
用户吃了"减脂午餐"食谱，额外加了一个苹果：
```json
POST /api/food/record/batch
{
  "recorded_at": "2025-11-03T12:30:00",
  "meal_type": "午餐",
  "foods": [
    {"food_name": "苹果", ...}
  ],
  "recipes": [
    {"recipe_name": "减脂午餐", ...}
  ]
}
```

### 场景5：查看今日营养摄入
```http
GET /api/food/record/daily-summary?date=2025-11-03
```

返回今日所有进食记录和总营养摄入，可与用户的每日卡路里目标对比。

### 场景6：与用户健康目标整合
```python
# 获取用户资料（包含每日卡路里目标）
user_profile = GET /api/user/profile
daily_goal = user_profile["daily_calorie_goal"]  # 例如：1800千卡

# 获取今日营养摄入
daily_summary = GET /api/food/record/daily-summary?date=2025-11-03
actual_intake = daily_summary["total_calories"]  # 例如：1650千卡

# 计算差值
remaining = daily_goal - actual_intake  # 还可以摄入150千卡
```

---

## 数据库结构

### 集合1: `foods` - 食物信息库
存储所有食物的基本信息和标准营养数据。

**字段说明**：
- `name`: 食物名称
- `category`: 分类（水果、蔬菜、肉类等）
- `serving_size`: 标准份量（数值）
- `serving_unit`: 份量单位
- `nutrition_per_serving`: 每份营养数据
- `brand`: 品牌（可选）
- `barcode`: 条形码（可选）
- `image_url`: 图片URL（可选）
- `created_by`: 创建者（系统食物为null）
- `created_at`: 创建时间
- `updated_at`: 更新时间

**索引**：`name`, `category`, `created_by`, `(name, created_by)`

### 集合2: `food_records` - 食物记录
存储用户的食物摄入记录。

**字段说明**：
- `user_email`: 用户邮箱
- `food_name`: 食物名称
- `serving_amount`: 食用份量数
- `serving_size`: 每份大小
- `serving_unit`: 份量单位
- `nutrition_data`: 实际摄入的营养数据（快照）
- `recorded_at`: 摄入时间（用户提供）
- `meal_type`: 餐次类型（早餐/午餐/晚餐/加餐）
- `notes`: 备注
- `food_id`: 关联的食物ID（可选）
- `created_at`: 记录时间（系统生成）

**索引**：`user_email`, `recorded_at`, `(user_email, recorded_at)`

### 集合3: `recipes` - 食谱库
存储所有食谱数据。

**字段说明**：
- `name`: 食谱名称
- `description`: 食谱描述
- `category`: 分类（早餐、午餐、晚餐等）
- `foods`: 食物列表（每个食物包含份量和营养信息）
- `total_nutrition`: 总营养数据（自动计算）
- `tags`: 标签列表
- `image_url`: 图片URL（可选）
- `prep_time`: 准备时间（分钟，可选）
- `created_by`: 创建者邮箱（系统食谱为null）
- `created_at`: 创建时间
- `updated_at`: 更新时间

**索引**：`name`, `category`, `created_by`, `(name, created_by)`

---

## 技术实现

### 文件结构
```
backend/app/
├── models/
│   ├── recipe.py              # 食谱数据模型
│   └── food.py                # 食物数据模型
├── schemas/
│   ├── recipe.py              # 食谱 API Schema
│   └── food.py                # 食物 API Schema
├── services/
│   ├── recipe_service.py      # 食谱业务逻辑
│   └── food_service.py        # 食物业务逻辑
├── routers/
│   ├── recipe.py              # 食谱路由
│   └── food.py                # 食物路由
└── init_db/
    ├── init_recipe_data.py    # 食谱初始化脚本
    └── init_food_data.py      # 食物初始化脚本
```

### 核心服务函数

#### recipe_service.py
- `create_recipe()` - 创建食谱
- `search_recipes()` - 搜索食谱
- `get_recipe_by_id()` - 获取详情
- `update_recipe()` - 更新食谱
- `delete_recipe()` - 删除食谱
- `calculate_recipe_nutrition()` - 计算总营养

#### food_service.py
- `create_food()` - 创建食物
- `search_foods()` - 搜索食物
- `create_food_record()` - 创建单个记录
- `create_batch_food_records()` - 批量创建记录
- `get_daily_nutrition_summary()` - 获取每日营养摘要
- `calculate_total_nutrition_from_list()` - 从营养列表计算总和

### 营养计算
```python
# 食谱总营养 = 所有食物的营养之和
total_nutrition = sum([food.nutrition for food in recipe.foods])

# 批量记录总营养 = 所有食物的营养之和
total_nutrition = sum([food.nutrition_data for food in foods]) +
                  sum([food.nutrition_data for recipe in recipes for food in recipe.foods])
```

### 技术特点
1. **异步处理**：所有数据库操作使用 `async/await`，Motor异步MongoDB驱动
2. **数据验证**：Pydantic模型自动验证，范围检查，类型安全
3. **查询优化**：MongoDB索引优化，分页查询支持

---

## 前端集成建议

### UI 设计

#### 食谱库页面
- 列表展示所有可用食谱
- 支持搜索和筛选
- 显示食谱详情（食物列表、总营养）
- 提供"创建食谱"、"编辑"、"删除"按钮

#### 食物记录页面
```
┌─────────────────────────────────┐
│  摄入时间: [2025-11-03 12:30]   │
│  餐次类型: [午餐]               │
├─────────────────────────────────┤
│  [添加食物] [添加食谱]          │
├─────────────────────────────────┤
│  已选择:                        │
│  • 鸡胸肉 150g (248 kcal)       │
│  • 白米饭 100g (116 kcal)       │
│  • [食谱] 轻食沙拉 (176 kcal)   │
├─────────────────────────────────┤
│  总计: 540 kcal                 │
│       蛋白质: 52g               │
│       碳水: 45g                 │
│       脂肪: 12g                 │
├─────────────────────────────────┤
│  备注: [_________________]      │
│                                 │
│  [取消]           [提交记录]    │
└─────────────────────────────────┘
```

### 数据流程

#### 使用食谱记录的流程
1. 用户点击"添加食谱"
2. 弹出食谱选择器，调用 `GET /api/recipe/search`
3. 用户选择食谱，前端调用 `GET /api/recipe/{id}` 获取详情
4. 前端展示食谱中的食物，允许调整 `scale`
5. 前端根据 `scale` 重新计算所有营养数据
6. 用户提交，前端调用 `POST /api/food/record/batch`

#### 关键代码示例
```javascript
// 调整份量倍数
function scaleRecipe(recipe, scale) {
  return {
    recipe_id: recipe.id,
    recipe_name: recipe.name,
    scale: scale,
    foods: recipe.foods.map(food => ({
      ...food,
      serving_amount: food.serving_amount * scale,
      nutrition_data: scaleNutrition(food.nutrition, scale)
    }))
  };
}

// 营养倍数调整
function scaleNutrition(nutrition, scale) {
  return {
    calories: nutrition.calories * scale,
    protein: nutrition.protein * scale,
    carbohydrates: nutrition.carbohydrates * scale,
    fat: nutrition.fat * scale,
    fiber: nutrition.fiber * scale,
    sodium: nutrition.sodium * scale
  };
}
```

---

## 注意事项

1. **营养计算**：记录食物时，`nutrition_data` 应该是实际摄入量（份量数 × 每份营养）
2. **权限控制**：
   - **食物**：系统食物（`created_by=null`）所有用户可见，用户创建的食物只有创建者自己可见（私有）
   - **食谱**：系统食谱（`created_by=null`）所有用户可见，用户创建的食谱只有创建者自己可见（私有）
   - **食物记录**：仅所属用户可见
3. **数据快照**：食物记录保存时会快照营养数据，避免后续修改食物信息影响历史记录
4. **时区处理**：所有时间使用UTC，前端需要转换为本地时间
5. **食谱中的食物必须来自食物库**：确保 `food_id` 正确
6. **重名检查**：食物和食谱名称不能重复
7. **前端需处理 scale**：调整食谱份量时前端负责计算

---

## 初始化数据

### 初始化食物数据库
```bash
cd backend
python -m app.init_db.init_food_data
```

这会添加12种常见食物到数据库。

### 初始化食谱数据库
```bash
cd backend
python -m app.init_db.init_recipe_data
```

这会添加6个系统默认食谱到数据库。

---

## API 文档

启动服务后，访问以下地址查看完整API文档：
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

