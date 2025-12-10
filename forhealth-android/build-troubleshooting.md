# Gradle 构建问题排查指南

## 如果构建卡住，请按以下步骤操作：

### 1. 立即操作（在 Android Studio 中）

1. **停止当前构建**
   - 点击 `Build` 菜单 → `Cancel Build`
   - 或点击工具栏的红色停止按钮

2. **检查网络连接**
   - 确保能访问 Maven Central 和 Google Maven
   - 如果在国内，可能需要配置代理或使用镜像

3. **清理 Gradle 缓存**
   ```
   在 Android Studio Terminal 中执行：
   ./gradlew clean --stop
   ```

### 2. 使用国内镜像（如果网络慢）

在项目根目录的 `build.gradle.kts` 中添加镜像源：

```kotlin
allprojects {
    repositories {
        // 阿里云镜像（国内推荐）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        
        // 原始源（备用）
        google()
        mavenCentral()
    }
}
```

### 3. 手动下载依赖（如果自动下载失败）

1. 打开 `File` → `Settings` → `Build, Execution, Deployment` → `Gradle`
2. 取消勾选 `Offline work`
3. 点击 `Apply` 和 `OK`
4. 重新同步项目

### 4. 检查 Android Studio 版本

- Android Gradle Plugin 8.2.0 需要：
  - Android Studio Hedgehog (2023.1.1) 或更高版本
  - Gradle 8.2 或更高版本

### 5. 如果仍然卡住

1. **关闭 Android Studio**
2. **删除以下文件夹**（在项目目录外）：
   - `C:\Users\20322\.gradle\caches\` （删除 caches 文件夹）
   - `C:\Users\20322\.gradle\daemon\` （删除 daemon 文件夹）
3. **重新打开 Android Studio**
4. **重新同步项目**

### 6. 查看详细日志

在 Android Studio 的 `Build` 窗口中，查看具体卡在哪一步：
- 如果卡在 "Downloading..." → 网络问题
- 如果卡在 "Resolving dependencies..." → 依赖冲突
- 如果卡在 "Building..." → 编译问题

### 7. 临时解决方案

如果急需继续开发，可以：
1. 先注释掉部分依赖
2. 只保留核心依赖（Core Android、Material）
3. 逐步添加其他依赖

