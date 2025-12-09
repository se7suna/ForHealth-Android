# API对接进度记录

## 重要架构说明

### 数据流架构
- **Models** (`models/`): 只服务于前端，用于UI显示
- **DTOs** (`network/dto/`): 只服务于后端，用于API通信
- **ViewModels**: 作为数据转换层（包装员）
  - **读取时**: Repository (DTO) → ViewModel (转换为 Model) → Fragment/Adapter
  - **写入时**: Fragment/Adapter (Model) → ViewModel (转换为 DTO) → Repository
- **后端允许字段为null**

### 数据转换规则
- ViewModel 负责 DTO ↔ Model 的转换
- Fragment/Adapter 只使用 Model
- Repository 只使用 DTO

## 阶段1：个人信息模块 ✅ 已完成

### 已完成的工作：
- [x] 检查并完善UserDTOs.kt，确保与api.txt完全一致
- [x] 创建UserRepository.kt，实现获取/更新用户资料、体重记录等所有方法
- [x] 修改ProfileFragment，移除直接API调用，改为通过UserRepository
- [x] 修改EditProfileFragment，移除直接API调用，改为通过UserRepository
- [x] 修改EditAccountActivity，移除直接API调用，改为通过UserRepository
- [x] 修改EditDataActivity，移除直接API调用，改为通过UserRepository
- [x] WeightTrackerFragment - ✅ 已完成：使用真实后端数据
  - [x] 当前体重：读取体重记录中最近期的记录
  - [x] 起始体重：读取体重记录中最早期的一条记录
  - [x] 目标体重：读取健康目标中的目标体重
  - [x] 绘制变化图时仅显示起始日期到终止日期的记录画成点
  - [x] 在用户修改了起始/终止日期时要刷新图

## 阶段2：饮食记录模块 ✅ 基本完成

### 架构要求（重要）：
- **ViewModel 作为数据转换层**：
  - 读取时：Repository (DTO) → ViewModel (转换为 Model) → Fragment/Adapter
  - 写入时：Fragment/Adapter (Model) → ViewModel (转换为 DTO) → Repository
- **Models** 只用于前端显示
- **DTOs** 只用于与后端通信
- **后端允许字段为null**

### 已完成工作：

#### 1. DTO层（与后端API对齐）
- [x] **FoodDTOs.kt** - 已完善，确保与api.txt完全一致
  - `FoodRecordCreateRequest` - 创建食物记录请求（已修复，移除了source字段）
  - `FoodRecordResponse` - 食物记录响应
  - `FoodRecordUpdateRequest` - 更新食物记录请求
  - `FoodRecordListResponse` - 食物记录列表响应
  - `FoodResponse` - 食物详情响应
  - `FoodSearchItemResponse` / `SimplifiedFoodSearchItem` - 食物搜索结果
  - `NutritionData` - 营养数据
  - 其他相关DTO

#### 2. Repository层（后端交互）
- [x] **FoodRepository.kt** - 已实现所有食物相关方法（只返回DTO）
  - `searchFoods()` - 搜索食物（薄荷健康数据库）
  - `searchFoodById()` - 通过名称搜索本地数据库
  - `getFood()` - 根据ID获取食物详情
  - `createFood()` - 创建自定义食物
  - `updateFood()` - 更新食物信息
  - `deleteFood()` - 删除食物
  - `updateFoodImage()` - 更新食物图片
  - `createFoodRecord()` - 创建食物记录
  - `getFoodRecords()` - 获取食物记录列表
  - `updateFoodRecord()` - 更新食物记录
  - `deleteFoodRecord()` - 删除食物记录
  - `getDailyNutrition()` - 获取某日营养摘要
  - `recognizeBarcode()` - 从图片识别条形码
  - `scanBarcode()` - 扫描条形码查询食品信息

- [x] **MealRepository.kt** - 已实现所有餐食相关方法（只返回DTO）
  - `getTodayMeals()` - 获取今日餐食列表
  - `getMeals()` - 获取指定日期范围的餐食列表
  - `createFoodRecord()` - 创建食物记录（添加餐食）
  - `createFoodRecords()` - 批量创建食物记录
  - `updateFoodRecord()` - 更新食物记录
  - `deleteFoodRecord()` - 删除食物记录
  - `getDailyNutrition()` - 获取某日的营养摘要

#### 3. ViewModel层（数据转换）
- [x] **MainViewModel.kt** - 已实现所有数据转换和业务逻辑方法
  - **DTO → Model 转换**：
    - `foodRecordResponseToMealItem()` - FoodRecordResponse → MealItem
  - **Model → DTO 转换**：
    - `mealItemToFoodRecordCreateRequest()` - MealItem → FoodRecordCreateRequest
    - `mealItemToFoodRecordUpdateRequest()` - MealItem → FoodRecordUpdateRequest
    - `selectedFoodItemToCreateRequest()` - SelectedFoodItem → FoodRecordCreateRequest
  - **数据类型转换**：
    - `mealTypeStringToEnum()` - 后端meal_type字符串（"早餐"/"午餐"/"晚餐"/"加餐"）→ 前端MealType枚举
    - `mealTypeEnumToString()` - 前端MealType枚举 → 后端meal_type字符串
  - **数据加载方法**：
    - `loadTodayMeals()` - 从后端加载今日餐食数据，转换为Model并更新本地状态
  - **数据创建方法**：
    - `createMealRecord()` - 创建单个食物记录
    - `createMealRecords()` - 批量创建食物记录（逐条上传），自动更新本地状态
  - **数据更新方法**：
    - `updateMealRecord()` - 更新单个食物记录
    - `updateMealGroupRecords()` - 更新餐食组记录（智能处理：更新现有记录、删除多余记录、创建新记录）
  - **数据删除方法**：
    - `deleteMealRecord()` - 删除食物记录（按时间戳分组，删除同一时间戳的所有记录）

#### 4. UI层（Fragment/Activity）
- [x] **HomeFragment.kt** - 已对接后端
  - 在 `onViewCreated()` 中调用 `viewModel.loadTodayMeals()` 加载今日餐食数据
  - 观察 `viewModel.meals` LiveData 更新UI显示
  - 观察 `viewModel.dailyStats` LiveData 更新统计数据

- [x] **AddMealFragment.kt** - 已对接后端
  - 使用 `FoodRepository.searchFoods()` 搜索食物（支持简化版和完整版）
  - 将搜索结果转换为 `FoodItem` Model 供UI显示
  - 调用 `mainViewModel.createMealRecords()` 批量创建食物记录
  - 处理创建成功/失败的回调

- [x] **EditMealFragment.kt** - 已对接后端
  - 使用 `FoodRepository.searchFoods()` 搜索食物
  - 调用 `mainViewModel.updateMealGroupRecords()` 更新餐食组记录
  - 调用 `mainViewModel.deleteMealRecord()` 删除餐食记录
  - 处理更新/删除成功/失败的回调

- [x] **CustomFoodFragment.kt** - 已对接后端
  - 使用 `FoodRepository.createFood()` 创建自定义食物
  - 将 `FoodResponse` DTO 转换为 `FoodItem` Model
  - 处理创建成功/失败的回调

#### 5. 数据流完整性验证
- [x] **读取流程**：Repository (DTO) → ViewModel (转换为 Model) → Fragment/Adapter ✅
  - HomeFragment 通过 `loadTodayMeals()` 加载数据
  - ViewModel 将 `FoodRecordListResponse` 转换为 `List<MealItem>`
  - Fragment 观察 `meals` LiveData 更新UI

- [x] **写入流程**：Fragment/Adapter (Model) → ViewModel (转换为 DTO) → Repository ✅
  - AddMealFragment 通过 `createMealRecords()` 创建记录
  - EditMealFragment 通过 `updateMealGroupRecords()` 更新记录
  - ViewModel 将 `SelectedFoodItem` / `MealItem` 转换为 `FoodRecordCreateRequest` / `FoodRecordUpdateRequest`
  - Repository 调用后端API

### 待完成工作：
- [ ] **CameraActivity** - AI食物识别功能（属于阶段4：AI模块）
  - 需要创建 AiRepository 并实现AI识别相关方法
  - 将识别结果转换为食物记录

### 备注：
- 饮食记录功能的前后端对接已基本完成
- 所有数据流都遵循架构要求：Model ↔ ViewModel ↔ DTO ↔ Repository
- 支持的功能包括：搜索食物、创建自定义食物、添加餐食、编辑餐食、删除餐食、查看今日餐食

## 阶段3：运动记录模块 ⏳ 待开始

- [ ] 检查并完善SportsDTOs.kt，确保与api.txt完全一致
- [ ] 完善ExerciseRepository.kt，实现获取运动列表、搜索运动、创建运动、记录运动等方法
- [ ] 删除WorkoutGroup相关代码，合并到Exercise中
- [ ] 修改AddExerciseFragment、EditExerciseFragment、CustomSportFragment，移除直接使用Constants，改为通过Repository

## 阶段4：AI模块 ⏳ 待开始

- [ ] 检查并完善AiDTOs.kt，确保与api.txt完全一致
- [ ] 创建AiRepository.kt，实现AI识别食物、问答、饮食分析等方法
- [ ] 修改AiChatFragment、CameraActivity中的AI调用，改为通过AiRepository

