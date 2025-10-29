# ForHealth Frontend

## 项目概述

ForHealth 是一款健康管理类 Android 应用，主要功能为用户录入个人身体信息、活动水平和健康目标，并提供个性化健康数据管理。  
应用采用 **本地模拟登录 + 后端 API 模拟** 的方式开发，界面风格简洁，交互流畅。

### 核心功能
1. **用户登录**
   - 模拟登录功能，仅支持账号 `test@example.com` / 密码 `123456`
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
└── README.md                                  # 项目文档
```


### 测试方式
1. 打开 Android Studio，加载项目
2. 运行应用（模拟器或真机）
3. 登录测试：
   - 输入 `test@example.com` / `123456` → 登录成功
   - 输入其他邮箱或密码 → 登录失败
4. 身体数据录入：
   - 滑动 NumberPicker 选择性别、出生年月日、身高、体重
   - 点击“确定”按钮进入下一步，完成后跳转到活动水平界面
5. 活动水平录入：
   - 滑动选择活动水平，查看下方提示文字变化
   - 点击“确定”按钮跳转到健康目标界面
6. 健康目标录入：
   - 设置目标类型、目标体重、目标周数 
   - 点击“确定”完成录入（当前仅 Toast 提示）

### 注意事项
- 目前仅支持测试账号登录：

```
邮箱: test@example.com
密码: 123456
```

- NumberPicker 的滑动切换动画仅在身体数据录入页面使用
- 身体数据录入、活动水平和健康目标均未实现持久化存储，仅在内存中模拟
- 后续可以接入真实后端 API 进行数据持久化和多用户管理
- 各界面可通过修改 XML 布局文件进行美化

### 未来优化方向
- 接入真实后端 API，实现注册、登录、数据存储
- 增加密码功能
- 增加用户信息编辑和历史记录查询
- UI 优化，响应式布局


