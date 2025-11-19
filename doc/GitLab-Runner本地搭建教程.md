# 本地搭建 GitLab Runner 教程（Windows）

## 📋 前置要求

- Windows 操作系统
- Docker Desktop（如果使用 docker executor）
- 管理员权限（可选，用于安装为系统服务）

## 🚀 快速开始

### 步骤 1: 下载 GitLab Runner

在 PowerShell 或 CMD 中运行：

```powershell
# 创建安装目录
mkdir D:\dev
cd D:\dev

# 下载最新版本的 GitLab Runner
curl -L --output gitlab-runner.exe "https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-windows-amd64.exe"
```

### 步骤 2: 在 GitLab UI 中创建 Runner

1. 访问项目的 CI/CD 设置页面：
   ```
   https://gitlab.com/<你的项目路径>/-/settings/ci_cd
   ```

2. 展开 **"Runners"** 部分

3. 点击 **"New project runner"**

4. 配置 Runner 属性：
   - **Tags**: 输入 `docker`（或其他自定义 tag）
   - **Description**: 自定义描述，如 `my-local-runner`
   - **Run untagged jobs**: 根据需要勾选
   - **Protected**: 通常不勾选

5. 点击 **"Create runner"**

6. **复制生成的令牌**（以 `glrt-` 开头）

### 步骤 3: 注册 Runner

使用步骤 2 获取的令牌注册 runner：

```powershell
cd D:\dev

# 基础注册命令
.\gitlab-runner.exe register --non-interactive `
  --url "https://gitlab.com/" `
  --token "glrt-YOUR_TOKEN_HERE" `
  --executor "docker" `
  --docker-image "python:3.11"
```

**参数说明**：
- `--url`: GitLab 实例地址
- `--token`: Runner 身份验证令牌（从 UI 复制）
- `--executor`: 执行器类型（docker/shell/等）
- `--docker-image`: 默认 Docker 镜像

**注册成功后会显示**：
```
Runner registered successfully. Feel free to start it!
Configuration saved in "D:\dev\config.toml"
```

### 步骤 4: 启动 Runner

#### 方式一：作为前台进程运行（推荐快速测试）

```powershell
cd D:\dev
.\gitlab-runner.exe run
```

**优点**：无需管理员权限
**缺点**：关闭窗口后 runner 停止

#### 方式二：安装为 Windows 服务（推荐生产环境）

**以管理员身份**运行 PowerShell：

```powershell
cd D:\dev

# 安装服务
.\gitlab-runner.exe install

# 启动服务
.\gitlab-runner.exe start

# 查看状态
.\gitlab-runner.exe status
```

## ✅ 验证 Runner 状态

### 方法 1: 命令行验证

```powershell
cd D:\dev

# 列出已注册的 runners
.\gitlab-runner.exe list

# 验证 runner 连接
.\gitlab-runner.exe verify
```

### 方法 2: GitLab UI 验证

访问项目的 CI/CD 设置页面，在 **Available specific runners** 部分应该能看到绿色的在线状态。

### 方法 3: 触发流水线测试

推送代码或创建 Merge Request，观察流水线是否被触发。

## 🔧 配置文件说明

配置文件位置：`D:\dev\config.toml`

示例配置：
```toml
concurrent = 1
check_interval = 0

[session_server]
  session_timeout = 1800

[[runners]]
  name = "my-local-runner"
  url = "https://gitlab.com/"
  token = "glrt-xxx"
  executor = "docker"
  [runners.docker]
    image = "python:3.11"
    privileged = false
    volumes = ["/cache"]
```

**常用配置项**：
- `concurrent`: 同时运行的最大作业数
- `executor`: 执行器类型
- `privileged`: Docker 特权模式（如需运行 Docker in Docker）
- `volumes`: 挂载的卷

## 🎯 .gitlab-ci.yml 配置

确保你的 `.gitlab-ci.yml` 中指定了正确的 tags：

```yaml
test:
  stage: test
  tags:
    - docker  # 必须与 runner 的 tags 匹配
  script:
    - echo "Running tests..."
```

## 🛠️ 常用命令

```powershell
# 启动 runner
.\gitlab-runner.exe start

# 停止 runner
.\gitlab-runner.exe stop

# 重启 runner
.\gitlab-runner.exe restart

# 查看状态
.\gitlab-runner.exe status

# 查看日志（如果作为服务运行）
.\gitlab-runner.exe --debug run

# 卸载服务
.\gitlab-runner.exe uninstall
```

## ⚠️ 常见问题

### 问题 1: "Access is denied" 错误

**原因**：安装服务需要管理员权限
**解决**：以管理员身份运行 PowerShell

### 问题 2: Runner 无法连接到 Docker

**原因**：Docker Desktop 未启动或配置不正确
**解决**：
1. 启动 Docker Desktop
2. 在 Docker Desktop 设置中启用 "Expose daemon on tcp://localhost:2375 without TLS"

### 问题 3: Runner 显示离线

**原因**：Runner 进程未运行或网络问题
**解决**：
```powershell
# 检查 runner 状态
.\gitlab-runner.exe status

# 重启 runner
.\gitlab-runner.exe restart
```

### 问题 4: 流水线没有被触发

**原因**：tags 不匹配
**解决**：确保 `.gitlab-ci.yml` 中的 tags 与 runner 配置的 tags 一致

## 📚 进阶配置

### 配置 Docker executor 使用特权模式

编辑 `config.toml`：
```toml
[[runners]]
  [runners.docker]
    privileged = true
    volumes = ["/var/run/docker.sock:/var/run/docker.sock"]
```

### 配置并发数

编辑 `config.toml`：
```toml
concurrent = 3  # 允许同时运行 3 个作业
```

### 配置缓存

编辑 `config.toml`：
```toml
[[runners]]
  [runners.docker]
    volumes = ["/cache", "D:/gitlab-runner-cache:/cache"]
```

## 🔗 参考链接

- [GitLab Runner 官方文档](https://docs.gitlab.com/runner/)
- [Windows 安装指南](https://docs.gitlab.com/runner/install/windows.html)
- [配置参考](https://docs.gitlab.com/runner/configuration/advanced-configuration.html)

## 💡 最佳实践

1. **使用特定的 tags**：避免使用 "run untagged jobs"，明确指定 tags
2. **限制并发数**：根据机器性能设置合理的 `concurrent` 值
3. **定期更新**：保持 GitLab Runner 版本最新
4. **监控日志**：定期查看 runner 日志，及时发现问题
5. **备份配置**：定期备份 `config.toml` 文件

---

**快速上手总结**：
```powershell
# 1. 下载
mkdir D:\dev && cd D:\dev
curl -L -o gitlab-runner.exe "https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-windows-amd64.exe"

# 2. 注册（在 GitLab UI 获取 token）
.\gitlab-runner.exe register --non-interactive --url "https://gitlab.com/" --token "glrt-xxx" --executor "docker" --docker-image "python:3.11"

# 3. 启动
.\gitlab-runner.exe run
```

就是这么简单！🚀
