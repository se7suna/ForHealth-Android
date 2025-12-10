# ForHealth - Diet Manager AI (Android)

健康饮食管理 Android 应用，从 React/TypeScript 前端完全迁移而来。

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/forhealth/
│   │   ├── models/          # 数据模型
│   │   ├── ui/              # UI组件
│   │   │   ├── activities/  # Activity
│   │   │   ├── fragments/   # Fragment
│   │   │   ├── adapters/    # RecyclerView适配器
│   │   │   └── views/       # 自定义View
│   │   ├── viewmodels/      # ViewModel
│   │   ├── repositories/    # 数据仓库
│   │   ├── network/         # 网络层
│   │   ├── utils/           # 工具类
│   │   └── ai/              # AI功能
│   └── res/                 # 资源文件
│       ├── layout/          # XML布局
│       ├── values/          # 颜色、字符串、尺寸等
│       └── drawable/         # 图标和drawable
```

## 技术栈

- **语言**: Kotlin
- **UI**: XML Layout + ViewBinding
- **架构**: MVVM (ViewModel + LiveData)
- **图片加载**: Coil
- **网络**: Retrofit + OkHttp
- **异步**: Coroutines

## 功能特性

- ✅ 饮食记录与追踪
- ✅ 运动记录与卡路里计算
- ✅ 实时统计数据
- ✅ 数据可视化（列表/图表视图）
- ✅ AI 智能建议（预留接口）
- ✅ 自定义食物/运动创建

## 开发环境要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK API 24+
- Gradle 8.2+

## 构建和运行

1. 在 Android Studio 中打开项目
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器
4. 点击 Run 按钮

## 待完成功能

- [ ] 完成所有 Fragment 实现
- [ ] 完成 Adapter 实现
- [ ] 完成 ViewModel 和 Repository
- [ ] 集成后端 API
- [ ] AI 功能集成
- [ ] 数据持久化

