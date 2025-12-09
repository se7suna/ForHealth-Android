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

## 阶段2：饮食记录模块 ⏳ 待开始

### 架构要求（重要）：
- **ViewModel 作为数据转换层**：
  - 读取时：Repository (DTO) → ViewModel (转换为 Model) → Fragment/Adapter
  - 写入时：Fragment/Adapter (Model) → ViewModel (转换为 DTO) → Repository
- **Models** 只用于前端显示
- **DTOs** 只用于与后端通信
- **后端允许字段为null**

### 已完成工作：
- [x] 检查并完善FoodDTOs.kt，确保与api.txt完全一致（已修复FoodRecordCreateRequest，移除了source字段）
- [x] 完善FoodRepository.kt，实现搜索食物、创建食物、获取食物记录等所有方法（只返回DTO）
- [x] 完善MealRepository.kt，实现获取今日餐食、添加餐食、删除餐食等方法（只返回DTO）
- [x] 在MainViewModel中添加DTO到Model的转换方法（FoodRecordResponse → MealItem）
- [x] 在MainViewModel中添加Model到DTO的转换方法（MealItem → FoodRecordCreateRequest/UpdateRequest）
- [x] 在MainViewModel中添加meal_type字符串与MealType枚举的转换方法
- [x] 在MainViewModel中添加从Repository加载数据的方法（loadTodayMeals、createMealRecord等）

### 待完成工作：
- [ ] 修改AddMealFragment、EditMealFragment、CustomFoodFragment，移除直接使用Constants，改为通过ViewModel
- [ ] 修改CameraActivity，移除直接API调用，改为通过ViewModel
- [ ] 修改MainActivity或其他调用MainViewModel的地方，调用loadTodayMeals()加载数据

## 阶段3：运动记录模块 ⏳ 待开始

- [ ] 检查并完善SportsDTOs.kt，确保与api.txt完全一致
- [ ] 完善ExerciseRepository.kt，实现获取运动列表、搜索运动、创建运动、记录运动等方法
- [ ] 删除WorkoutGroup相关代码，合并到Exercise中
- [ ] 修改AddExerciseFragment、EditExerciseFragment、CustomSportFragment，移除直接使用Constants，改为通过Repository

## 阶段4：AI模块 ⏳ 待开始

- [ ] 检查并完善AiDTOs.kt，确保与api.txt完全一致
- [ ] 创建AiRepository.kt，实现AI识别食物、问答、饮食分析等方法
- [ ] 修改AiChatFragment、CameraActivity中的AI调用，改为通过AiRepository

