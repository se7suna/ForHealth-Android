# Analytics模块前后端对接任务规划

## 概述

Analytics模块包含三种图表：
1. **柱状图（日视图）** - 显示今日摄入和消耗
2. **折线图（周/月视图）** - 显示多天的摄入和消耗趋势
3. **饼图（甜甜圈图）** - 显示宏量营养素比例（蛋白质、碳水、脂肪）

## 后端API对应关系

| 图表类型 | 后端API | 用途 |
|---------|---------|------|
| 柱状图（日视图） | `/api/visualization/daily-calorie-summary` | 获取指定日期的卡路里摘要 |
| 折线图（周/月视图） | `/api/visualization/time-series-trend` | 获取时间序列趋势（摄入/消耗） |
| 饼图（甜甜圈图） | `/api/visualization/nutrition-analysis` | 获取营养素分析（宏量营养素比例） |

## 数据流架构

遵循统一架构：**前端 Model → MainViewModel → Repository → DTO → 后端**

### 数据转换映射

1. **DailyCalorieSummary (DTO) → DailyStats (Model)**
   - `total_intake` → `calories.current`
   - `total_burned` → `burned`
   - `daily_goal` → `calories.target`

2. **TimeSeriesTrendResponse (DTO) → List<ChartDataPoint> (Model)**
   - `intake_trend` + `burned_trend` → 合并为 `ChartDataPoint` 列表
   - 每个数据点包含：`label`（日期）、`intake`、`burned`

3. **NutritionAnalysisResponse (DTO) → 宏量营养素数据 (Model)**
   - `macronutrient_ratio` → 用于饼图显示
   - 需要计算实际的卡路里值（百分比 × 总摄入）

## 任务清单

### 阶段1：Repository层（后端交互）

#### ✅ 任务1：创建 VisualizationRepository.kt
- [x] 实现 `getDailyCalorieSummary(targetDate: String?)` - 获取每日卡路里摘要
- [x] 实现 `getTimeSeriesTrend(startDate: String, endDate: String, viewType: String)` - 获取时间序列趋势
- [x] 实现 `getNutritionAnalysis(startDate: String, endDate: String)` - 获取营养素分析
- [x] 所有方法只返回DTO，不进行转换

### 阶段2：ViewModel层（数据转换）

#### ✅ 任务2：DTO → Model 转换方法
- [x] `timeSeriesTrendResponseToChartDataPoints()` - 将时间序列趋势转换为图表数据点
- [x] `nutritionAnalysisResponseToMacroData()` - 将营养素分析转换为宏量营养素数据
- [x] `dailyCalorieSummaryToDailyStats()` - 将每日摘要转换为DailyStats（可选，如果直接使用DailyStats）

#### ✅ 任务3：数据加载方法
- [x] `loadDailyCalorieSummary(targetDate: String?)` - 加载日视图数据
- [x] `loadTimeSeriesTrend(startDate: String, endDate: String, viewType: String)` - 加载周/月视图数据
- [x] `loadNutritionAnalysis(startDate: String, endDate: String)` - 加载饼图数据
- [x] 添加LiveData用于暴露数据给UI层

### 阶段3：UI层（Fragment）

#### ✅ 任务4：修改 HomeFragment
- [x] 修改 `updateAnalyticsDisplay()` 方法：
  - 日视图：使用 `loadDailyCalorieSummary()` 获取数据
  - 饼图：使用 `loadNutritionAnalysis()` 获取数据
- [x] 修改 `generateChartData()` 方法：
  - 周/月视图：使用 `loadTimeSeriesTrend()` 获取数据
- [x] 在 `setupAnalyticsListeners()` 中，根据 `currentRange` 调用相应的加载方法
- [x] 观察ViewModel的LiveData，自动更新图表

## 实现细节

### 1. 柱状图（日视图）

**当前实现**：从本地 `meals` 和 `exercises` 计算
**新实现**：调用 `daily-calorie-summary` API

```kotlin
// ViewModel
fun loadDailyCalorieSummary(targetDate: String? = null) {
    viewModelScope.launch {
        when (val result = visualizationRepository.getDailyCalorieSummary(targetDate)) {
            is ApiResult.Success -> {
                // 转换为DailyStats或直接使用
                // 更新LiveData
            }
        }
    }
}
```

### 2. 折线图（周/月视图）

**当前实现**：使用模拟数据（只有当前日使用真实数据）
**新实现**：调用 `time-series-trend` API

```kotlin
// ViewModel
fun loadTimeSeriesTrend(startDate: String, endDate: String, viewType: String) {
    viewModelScope.launch {
        when (val result = visualizationRepository.getTimeSeriesTrend(startDate, endDate, viewType)) {
            is ApiResult.Success -> {
                val chartData = timeSeriesTrendResponseToChartDataPoints(result.data)
                // 更新LiveData
            }
        }
    }
}
```

**数据转换逻辑**：
- 合并 `intake_trend` 和 `burned_trend`
- 按日期匹配，创建 `ChartDataPoint(label, intake, burned)`

### 3. 饼图（甜甜圈图）

**当前实现**：从本地 `meals` 计算宏量营养素
**新实现**：调用 `nutrition-analysis` API

```kotlin
// ViewModel
fun loadNutritionAnalysis(startDate: String, endDate: String) {
    viewModelScope.launch {
        when (val result = visualizationRepository.getNutritionAnalysis(startDate, endDate)) {
            is ApiResult.Success -> {
                val macroData = nutritionAnalysisResponseToMacroData(result.data)
                // 更新LiveData
            }
        }
    }
}
```

**数据转换逻辑**：
- 从 `macronutrient_ratio` 获取百分比
- 需要总摄入量来计算实际的卡路里值
- 可以使用 `daily-calorie-summary` 的 `total_intake` 或从 `nutrition-analysis` 的其他字段获取

## 注意事项

1. **日期范围计算**：
   - 日视图：使用当前日期
   - 周视图：计算最近7天的日期范围
   - 月视图：计算最近28天或30天的日期范围

2. **数据同步**：
   - 当用户切换范围（DAY/WEEK/MONTH）时，需要重新加载对应的数据
   - 当数据更新（添加/删除餐食或运动）时，需要刷新Analytics视图

3. **错误处理**：
   - API调用失败时，可以显示错误消息或使用默认值
   - 考虑添加加载状态指示器

4. **性能优化**：
   - 可以缓存已加载的数据，避免重复请求
   - 周/月视图的数据量较大，考虑分页或限制日期范围

## 测试要点

1. 日视图：验证柱状图显示正确的摄入和消耗数据
2. 周视图：验证折线图显示7天的趋势数据
3. 月视图：验证折线图显示多周的趋势数据
4. 饼图：验证甜甜圈图显示正确的宏量营养素比例
5. 范围切换：验证切换DAY/WEEK/MONTH时数据正确更新
6. 数据更新：验证添加/删除记录后图表自动刷新

