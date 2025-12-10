# API DTO 说明

## 目录结构

```
dto/
├── auth/          # 认证相关 DTO
├── user/          # 用户管理相关 DTO
├── sports/        # 运动记录相关 DTO
├── food/          # 食物管理相关 DTO
├── recipe/        # 食谱管理相关 DTO
└── visualization/ # 可视化报告相关 DTO
```

## 使用说明

所有 DTO 类都按照后端 OpenAPI 规范定义，字段名称与后端保持一致。

### 认证流程

1. 发送验证码：`SendRegistrationCodeRequest`
2. 用户注册：`UserRegisterRequest`
3. 用户登录：`UserLoginRequest` -> `TokenResponse`
4. 使用 Token：通过 `AuthInterceptor` 自动添加到请求头

### 数据模型映射

- **NutritionData**: 基础营养数据（卡路里、蛋白质、碳水、脂肪等）
- **FullNutritionData**: 完整营养信息（包含维生素、矿物质、氨基酸等）
- **FoodResponse**: 食物完整信息
- **FoodRecordResponse**: 食物记录信息

### 注意事项

1. **日期格式**: 
   - 日期使用 `YYYY-MM-DD` 格式
   - 日期时间使用 ISO 8601 格式（如：`2025-11-13T12:30:00`）

2. **Token 管理**:
   - Token 存储在 SharedPreferences 或 DataStore
   - 通过 `RetrofitClient.setTokenProvider()` 设置 Token 提供者
   - `AuthInterceptor` 自动在需要认证的请求中添加 Token

3. **错误处理**:
   - 使用 `ApiResult` 封装所有 API 调用结果
   - 统一处理成功/失败/加载状态

4. **分页**:
   - 使用 `limit` 和 `offset` 参数
   - 响应中包含 `total` 字段表示总数

