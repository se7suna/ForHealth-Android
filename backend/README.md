# For Health 后端服务

## 快速开始

### 1. 启动 Docker 服务（首次需要）

```bash
# 在项目根目录执行
cd ..
docker-compose up -d mongodb mailhog
cd backend
```

### 2. 启动后端服务

双击运行 `start.bat` 或在命令行执行：

```bash
start.bat
```

服务启动后访问：
- **API 文档**: http://127.0.0.1:8000/docs
- **MailHog UI**: http://localhost:8025

### 3. 运行测试（另开窗口）

双击运行 `test.bat` 或在新的命令行窗口执行：

```bash
test.bat
```

## 脚本说明

- **start.bat** - 启动后端服务
  - 自动创建虚拟环境
  - 安装依赖
  - 启动 FastAPI 服务

- **test.bat** - 运行测试
  - 激活虚拟环境
  - 执行 pytest 测试

## 注意事项

1. **首次运行前**，确保已复制 `.env` 文件：
   ```bash
   copy .env.example .env
   ```

2. **邮件功能**需要 MailHog 服务，确保 Docker 已启动：
   ```bash
   docker ps | findstr mailhog
   ```

3. **测试前**确保后端服务已启动（运行 start.bat）

## 常见问题

### 邮件发送失败

检查 `.env` 文件配置：
```env
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASSWORD=
```

### 端口被占用

修改 `.env` 文件中的端口：
```env
PORT=8001
```
