# API对接架构说明

## 架构设计

### 1. 网络层 (Network Layer)
- `ApiService.kt` - 定义所有API接口
- `RetrofitClient.kt` - Retrofit客户端配置
- `ApiResult.kt` - 统一的结果封装和错误处理
- `dto/` - 数据传输对象（请求/响应DTO）

### 2. 数据仓库层 (Repository Layer)
- `FoodRepository.kt` - 食物数据管理
- `ExerciseRepository.kt` - 运动数据管理
- `MealRepository.kt` - 餐食数据管理
- `StatsRepository.kt` - 统计数据管理

### 3. 数据流
```
UI (Fragment/Activity)
    ↓
ViewModel
    ↓
Repository
    ↓
ApiService (Retrofit)
    ↓
后端API
```

## 对接步骤

1. **接收API接口定义**
   - 在 `ApiService.kt` 中添加接口方法
   - 定义对应的请求/响应DTO

2. **实现Repository方法**
   - 调用 `ApiService` 中的接口
   - 使用 `safeApiCall` 进行错误处理
   - 返回 `ApiResult<T>`

3. **在ViewModel中使用**
   - ViewModel调用Repository
   - 使用LiveData/Flow暴露数据
   - 处理加载状态和错误

4. **在UI中观察**
   - Fragment/Activity观察ViewModel的LiveData
   - 根据ApiResult状态更新UI

## 待对接的API（根据后端提供）

- [ ] 获取食物列表
- [ ] 搜索食物
- [ ] 创建自定义食物
- [ ] 获取运动列表
- [ ] 搜索运动
- [ ] 创建自定义运动
- [ ] 添加餐食
- [ ] 获取今日餐食
- [ ] 删除餐食
- [ ] 获取统计数据
- [ ] 获取AI建议

## 注意事项

1. **Base URL配置**
   - 当前在 `RetrofitClient.kt` 中硬编码
   - 建议改为从 `BuildConfig` 或配置文件读取

2. **错误处理**
   - 使用 `ApiResult` 统一处理成功/失败
   - 在ViewModel中转换为UI可用的状态

3. **数据缓存**
   - Repository层可以添加本地数据库缓存
   - 优先使用本地数据，后台同步API数据

4. **认证**
   - 如果需要Token认证，在 `OkHttpClient` 中添加拦截器

