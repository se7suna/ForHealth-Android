# Docker 镜像拉取超时问题解决方案

## 📋 问题描述

### 场景 1: Windows 环境
在运行 GitLab CI/CD 时出现错误:
```
ERROR: Job failed: failed to pull image "mongo:latest" with specified policies [always]:
Error response from daemon: net/http: TLS handshake timeout
```

### 场景 2: Linux 服务器环境
在 Linux 服务器上执行 GitLab Runner 时,拉取 `python:3.11` 等镜像失败,提示找不到镜像,即使已配置了镜像源。

## 🔍 原因分析

1. **网络问题**: 访问 Docker Hub 网络不稳定
2. **超时设置**: 默认拉取超时时间过短
3. **镜像策略**: 每次都尝试从远程拉取最新镜像
4. **镜像源失效**: 中国大陆的公共 Docker 镜像源大多已停止服务
   - ❌ 中科大镜像 (`docker.mirrors.ustc.edu.cn`) 已于 2022 年停止
   - ❌ 网易镜像 (`hub-mirror.c.163.com`) 已停止服务
   - ⚠️ 华为云镜像 (`swr.cn-north-4.myhuaweicloud.com`) 需要认证配置

---

## 🐧 Linux 服务器解决方案

### 方案 1: 使用阿里云个人镜像加速器 ⭐强烈推荐

阿里云为每个用户提供免费的个人镜像加速服务,稳定可靠,不限流量。

#### 步骤 1: 获取专属加速地址

1. 登录阿里云控制台: https://cr.console.aliyun.com/
2. 进入 **容器镜像服务** > **镜像工具** > **镜像加速器**
3. 复制你的专属加速地址,格式如: `https://xxxxxx.mirror.aliyuncs.com`

![阿里云镜像加速器](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/9665359951/p7487.png)

#### 步骤 2: 配置 Docker 镜像源

```bash
# 编辑 Docker 配置文件
sudo vim /etc/docker/daemon.json
```

完整配置示例:
```json
{
  "registry-mirrors": [
    "https://你的专属ID.mirror.aliyuncs.com"
  ],
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

⚠️ **重要提示**:
- 必须使用你自己的专属加速地址,不能直接复制示例
- 如果文件不存在,创建一个新的: `sudo touch /etc/docker/daemon.json`
- 确保 JSON 格式正确(可用 `jq` 验证: `cat /etc/docker/daemon.json | jq .`)

#### 步骤 3: 重启 Docker 服务

```bash
# 重载系统配置
sudo systemctl daemon-reload

# 重启 Docker
sudo systemctl restart docker

# 检查 Docker 状态
sudo systemctl status docker
```

#### 步骤 4: 验证配置生效

```bash
# 查看镜像源配置
docker info | grep -A 5 "Registry Mirrors"

# 应该输出:
# Registry Mirrors:
#   https://xxxxxx.mirror.aliyuncs.com/

# 测试拉取镜像
docker pull python:3.11
docker pull mongo:latest
docker pull mailhog/mailhog:latest
```

#### 步骤 5: 重启 GitLab Runner

```bash
# 如果 Runner 是作为服务运行
sudo gitlab-runner restart

# 查看 Runner 状态
sudo gitlab-runner status

# 如果是前台运行,停止后重新启动
# Ctrl+C 停止,然后:
sudo gitlab-runner run
```

---

### 方案 2: 使用可用的国内公共镜像源

如果不想使用阿里云账号,可以尝试以下仍可用的镜像源:

```bash
sudo vim /etc/docker/daemon.json
```

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.nju.edu.cn",
    "https://docker.mirrors.sjtug.sjtu.edu.cn"
  ],
  "max-concurrent-downloads": 10
}
```

**可用镜像源列表** (2024年11月15日更新):

**稳定性较高的镜像源** (推荐优先使用):
- ✅ 1Panel: `https://docker.1panel.live`
- ✅ AtomHub: `https://atomhub.openatom.cn`
- ✅ DaoCloud: `https://docker.m.daocloud.io`
- ✅ 南京大学: `https://docker.nju.edu.cn`

**备选镜像源** (部分可能有限制):
- `https://docker.1ms.run`
- `https://hub.rat.dev`
- `https://docker.xiaogenban1993.com`
- `https://dockerpull.org`
- `https://dockerhub.icu`

**配置示例** (多个镜像源作为备选):
```json
{
  "registry-mirrors": [
    "https://docker.1panel.live",
    "https://atomhub.openatom.cn",
    "https://docker.m.daocloud.io",
    "https://docker.nju.edu.cn"
  ],
  "max-concurrent-downloads": 10
}
```

⚠️ **重要提示**:
- 自2024年6月起,阿里云、腾讯云、中科大等官方镜像源已停止公共服务
- 公共镜像源可能不稳定、有访问限制或仅支持白名单镜像
- **强烈推荐使用阿里云个人镜像加速器**(方案1),这是目前最稳定可靠的方案

---

### 方案 3: 修改 GitLab Runner 配置增加容错

编辑 Runner 配置文件(通常在 `/etc/gitlab-runner/config.toml`):

```bash
sudo vim /etc/gitlab-runner/config.toml
```

修改配置:
```toml
concurrent = 1

[[runners]]
  name = "my-runner"
  url = "https://gitlab.com/"
  token = "your-runner-token"
  executor = "docker"

  [runners.docker]
    tls_verify = false
    image = "python:3.11"
    privileged = false
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false
    volumes = ["/cache"]
    shm_size = 0

    # 镜像拉取策略:优先使用本地镜像
    pull_policy = ["if-not-present", "always"]

    # 增加镜像拉取超时时间(秒)
    pull_timeout = 600

    # 增加服务启动超时时间(秒)
    wait_for_services_timeout = 300
```

**pull_policy 说明**:
- `if-not-present`: 本地存在则使用本地镜像,不存在才拉取
- `always`: 总是尝试拉取最新镜像
- `never`: 只使用本地镜像,不拉取

然后重启 Runner:
```bash
sudo gitlab-runner restart
sudo gitlab-runner verify
```

---

### 方案 4: 在 CI/CD 中使用国内镜像仓库

修改 `.gitlab-ci.yml`,直接使用阿里云等国内镜像仓库:

```yaml
variables:
  # 使用阿里云镜像
  PYTHON_IMAGE: "registry.cn-hangzhou.aliyuncs.com/library/python:3.11"
  MONGO_IMAGE: "registry.cn-hangzhou.aliyuncs.com/library/mongo:latest"

test:unit:
  stage: test
  image: ${PYTHON_IMAGE}
  services:
    - name: ${MONGO_IMAGE}
      alias: mongo
    - name: mailhog/mailhog:latest
      alias: mailhog
  # ... 其他配置
```

**国内镜像仓库列表**:
- 阿里云杭州: `registry.cn-hangzhou.aliyuncs.com/library/`
- 阿里云北京: `registry.cn-beijing.aliyuncs.com/library/`
- 腾讯云: `ccr.ccs.tencentyun.com/library/`

---

### 方案 5: 配置代理(如有代理服务器)

如果服务器有代理,可以配置 Docker 使用代理访问 Docker Hub。

```bash
# 创建代理配置目录
sudo mkdir -p /etc/systemd/system/docker.service.d

# 创建代理配置文件
sudo vim /etc/systemd/system/docker.service.d/http-proxy.conf
```

添加内容:
```ini
[Service]
Environment="HTTP_PROXY=http://your-proxy-server:port"
Environment="HTTPS_PROXY=http://your-proxy-server:port"
Environment="NO_PROXY=localhost,127.0.0.1,docker-registry.example.com"
```

重启 Docker:
```bash
sudo systemctl daemon-reload
sudo systemctl restart docker

# 验证代理配置
sudo systemctl show --property=Environment docker
```

---

### 🎯 推荐组合方案(Linux)

**最佳实践**: 方案 1 + 方案 3

1. ✅ 配置阿里云个人镜像加速器(解决网络问题)
2. ✅ 修改 Runner 配置增加容错机制(提高稳定性)
3. ✅ 预先手动拉取常用镜像(加速首次执行)

```bash
# 配置完镜像加速器后,预拉取 CI/CD 常用镜像
docker pull python:3.11
docker pull mongo:latest
docker pull mailhog/mailhog:latest

# 验证镜像已存在
docker images | grep -E 'python|mongo|mailhog'

# 重启 Runner
sudo gitlab-runner restart
```

---

## 🪟 Windows 环境解决方案

### 方案一: 配置 Docker Desktop 镜像加速器(推荐)

#### 1. 打开 Docker Desktop 设置

1. 右键点击系统托盘的 Docker 图标
2. 选择 **Settings** (设置)
3. 进入 **Docker Engine** 页面

#### 2. 添加镜像加速器配置

在 JSON 配置中添加以下内容:

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.nju.edu.cn",
    "https://dockerhub.timeweb.cloud",
    "https://noohub.ru"
  ],
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5
}
```

**国内常用镜像加速器**:
- DaoCloud: `https://docker.m.daocloud.io`
- 南京大学: `https://docker.nju.edu.cn`
- 上海交大: `https://docker.mirrors.sjtug.sjtu.edu.cn`

#### 3. 应用配置

点击 **Apply & Restart** 重启 Docker Desktop

### 方案二: 修改 GitLab Runner 配置

编辑 `D:\dev\config.toml`:

```toml
[[runners]]
  [runners.docker]
    # 使用 if-not-present 策略,优先使用本地镜像
    pull_policy = ["if-not-present"]

    # 增加服务启动超时时间(秒)
    wait_for_services_timeout = 300

    # 增加镜像拉取超时时间(秒)
    pull_timeout = 600
```

**拉取策略说明**:
- `always`: 总是尝试拉取最新镜像(默认,可能导致超时)
- `if-not-present`: 本地有则使用本地,没有才拉取
- `never`: 只使用本地镜像,不拉取

### 方案三: 预先拉取所需镜像

在本地手动拉取 CI/CD 所需的所有镜像:

```powershell
# 拉取 Python 镜像
docker pull python:3.11

# 拉取 MongoDB 镜像
docker pull mongo:latest

# 拉取 MailHog 镜像
docker pull mailhog/mailhog:latest
```

这样 Runner 执行时会直接使用本地镜像,避免网络问题。

### 方案四: 使用国内镜像源(临时方案)

修改 `.gitlab-ci.yml`,使用国内镜像:

```yaml
test:unit:
  stage: test
  image: registry.cn-hangzhou.aliyuncs.com/library/python:3.11  # 使用阿里云镜像
  services:
    - registry.cn-hangzhou.aliyuncs.com/library/mongo:latest
    - mailhog/mailhog:latest  # MailHog 保持原样
```

---

## 📝 完整操作步骤

### Linux 服务器完整配置流程

```bash
# 1. 配置阿里云镜像加速器(替换为你的专属地址)
sudo tee /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
    "https://你的专属ID.mirror.aliyuncs.com"
  ],
  "max-concurrent-downloads": 10
}
EOF

# 2. 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker

# 3. 验证配置
docker info | grep -A 5 "Registry Mirrors"

# 4. 预拉取镜像
docker pull python:3.11
docker pull mongo:latest
docker pull mailhog/mailhog:latest

# 5. 修改 Runner 配置(可选)
sudo vim /etc/gitlab-runner/config.toml
# 添加 pull_policy = ["if-not-present"]

# 6. 重启 Runner
sudo gitlab-runner restart

# 7. 验证 Runner 状态
sudo gitlab-runner status
```

### Windows 完整配置流程

```powershell
# 1. 配置 Docker Desktop 加速器
# 打开 Docker Desktop → Settings → Docker Engine
# 添加镜像加速器配置 → Apply & Restart

# 2. 修改 Runner 配置
# 编辑 D:\dev\config.toml
# 添加 pull_policy = ["if-not-present"]

# 3. 预先拉取镜像
docker pull python:3.11
docker pull mongo:latest
docker pull mailhog/mailhog:latest

# 4. 重启 GitLab Runner
cd D:\dev
.\gitlab-runner.exe restart

# 5. 重新触发流水线
# 在 GitLab Merge Request 页面点击 Retry
```

---

## ✅ 验证配置

### 检查 Docker 镜像加速器

**Linux**:
```bash
# 查看 Docker 配置
docker info | grep -A 5 "Registry Mirrors"

# 应该输出:
# Registry Mirrors:
#   https://xxxxxx.mirror.aliyuncs.com/
```

**Windows**:
```powershell
# 查看 Docker 配置
docker info

# 输出中应该包含:
# Registry Mirrors:
#   https://docker.m.daocloud.io/
```

### 检查本地镜像

```bash
# 列出已拉取的镜像
docker images

# 应该看到:
# REPOSITORY          TAG       IMAGE ID       CREATED        SIZE
# python              3.11      ...            ...            ...
# mongo               latest    ...            ...            ...
# mailhog/mailhog     latest    ...            ...            ...
```

### 测试镜像拉取速度

```bash
# 删除测试镜像(如果存在)
docker rmi hello-world

# 测试拉取速度
time docker pull hello-world

# 成功且耗时短则说明配置生效
```

### 检查 GitLab Runner 配置

**Linux**:
```bash
# 查看 Runner 配置
sudo cat /etc/gitlab-runner/config.toml

# 验证 Runner 状态
sudo gitlab-runner verify
```

**Windows**:
```powershell
# 查看 Runner 配置
cat D:\dev\config.toml

# 验证 Runner 状态
cd D:\dev
.\gitlab-runner.exe verify
```

---

## ❓ 常见问题

### Q1: 配置阿里云加速器后仍然拉取失败

**可能原因**:
1. ❌ 镜像源地址配置错误
2. ❌ Docker 服务未重启
3. ❌ JSON 格式错误
4. ❌ 网络防火墙限制

**解决方法**:
```bash
# 1. 检查配置文件语法
cat /etc/docker/daemon.json | jq .

# 2. 检查 Docker 日志
sudo journalctl -u docker.service -n 50

# 3. 手动测试拉取
docker pull python:3.11

# 4. 检查网络连接
curl -I https://你的专属ID.mirror.aliyuncs.com

# 5. 尝试其他镜像源
sudo vim /etc/docker/daemon.json
# 添加多个镜像源作为备选
```

### Q2: Runner 配置修改后不生效

**解决方法**:

**Linux**:
```bash
# 重启 Runner
sudo gitlab-runner restart

# 验证配置
sudo gitlab-runner verify

# 如果还不行,重新注册 Runner
sudo gitlab-runner unregister --all-runners
sudo gitlab-runner register
```

**Windows**:
```powershell
# 重启 Runner
cd D:\dev
.\gitlab-runner.exe restart

# 如果是服务方式运行
.\gitlab-runner.exe stop
.\gitlab-runner.exe start
```

### Q3: 镜像拉取速度仍然很慢

**优化建议**:
1. ✅ 使用有线网络而非 Wi-Fi
2. ✅ 关闭 VPN 或代理(如果镜像源在国内)
3. ✅ 尝试在网络空闲时段拉取镜像
4. ✅ 增加并发下载数: `"max-concurrent-downloads": 10`
5. ✅ 使用阿里云个人加速器而非公共镜像源

### Q4: 找不到 `/etc/docker/daemon.json` 文件

**解决方法**:
```bash
# 创建文件
sudo touch /etc/docker/daemon.json

# 添加配置
sudo tee /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
    "https://你的专属ID.mirror.aliyuncs.com"
  ]
}
EOF

# 重启 Docker
sudo systemctl restart docker
```

### Q5: GitLab CI 中 services 镜像拉取失败

**解决方法**:

在 `.gitlab-ci.yml` 中指定国内镜像源:
```yaml
test:unit:
  stage: test
  image: registry.cn-hangzhou.aliyuncs.com/library/python:3.11
  services:
    - name: registry.cn-hangzhou.aliyuncs.com/library/mongo:latest
      alias: mongo
    - name: mailhog/mailhog:latest  # 如果拉取失败,预先在服务器上拉取
      alias: mailhog
```

或预先在服务器上拉取:
```bash
docker pull mailhog/mailhog:latest
```

---

## 🎯 推荐配置(综合方案)

### Linux 推荐配置

**Docker 配置** (`/etc/docker/daemon.json`):
```json
{
  "registry-mirrors": [
    "https://你的专属ID.mirror.aliyuncs.com"
  ],
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "dns": ["8.8.8.8", "114.114.114.114"]
}
```

**Runner 配置** (`/etc/gitlab-runner/config.toml`):
```toml
concurrent = 1

[[runners]]
  name = "my-linux-runner"
  url = "https://gitlab.com/"
  token = "your-runner-token"
  executor = "docker"

  [runners.docker]
    tls_verify = false
    image = "python:3.11"
    privileged = false
    pull_policy = ["if-not-present", "always"]
    pull_timeout = 600
    wait_for_services_timeout = 300
    volumes = ["/var/run/docker.sock:/var/run/docker.sock", "/cache"]
```

### Windows 推荐配置

**Docker Desktop 配置**:
```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.nju.edu.cn"
  ],
  "max-concurrent-downloads": 10,
  "dns": ["8.8.8.8", "114.114.114.114"]
}
```

**Runner 配置** (`config.toml`):
```toml
concurrent = 1

[[runners]]
  name = "my-windows-runner"
  url = "https://gitlab.com/"
  token = "your-runner-token"
  executor = "docker"

  [runners.docker]
    image = "python:3.11"
    pull_policy = ["if-not-present"]
    wait_for_services_timeout = 300
    privileged = true
    volumes = ["/var/run/docker.sock:/var/run/docker.sock", "/cache"]
```

### .gitlab-ci.yml 优化

```yaml
# 使用变量控制镜像源
variables:
  PYTHON_VERSION: "3.11"
  DOCKER_DRIVER: overlay2
  DOCKER_TLS_CERTDIR: ""
  # 增加拉取超时时间
  GET_SOURCES_ATTEMPTS: 3
  RESTORE_CACHE_ATTEMPTS: 3

test:unit:
  stage: test
  image: python:${PYTHON_VERSION}
  services:
    - name: mongo:latest
      alias: mongo
    - name: mailhog/mailhog:latest
      alias: mailhog
  tags:
    - docker
  # 增加重试机制
  retry:
    max: 2
    when:
      - runner_system_failure
      - stuck_or_timeout_failure
  timeout: 30m
  before_script:
    - echo "使用本地镜像,避免重复拉取"
    - cd backend
    - pip install --upgrade pip
    - pip install -r requirements.txt
  script:
    - pytest tests/ -v
```

---

## 📊 总结

### 最有效的组合方案

**Linux 服务器**:
1. ✅ 配置阿里云个人镜像加速器
2. ✅ 设置 pull_policy = ["if-not-present", "always"]
3. ✅ 预先拉取所需镜像
4. ✅ 增加超时时间配置
5. ✅ 在 CI/CD 中增加重试机制

**Windows 环境**:
1. ✅ 配置 Docker Desktop 镜像加速器
2. ✅ 设置 pull_policy = ["if-not-present"]
3. ✅ 预先拉取所需镜像
4. ✅ 增加超时时间配置

这样可以**大幅提高 CI/CD 执行速度**,避免网络超时问题。

---

## 🚀 快速修复命令

### Linux 快速修复

```bash
# 一键配置(替换为你的阿里云镜像地址)
sudo tee /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["https://你的专属ID.mirror.aliyuncs.com"],
  "max-concurrent-downloads": 10
}
EOF

# 重启服务
sudo systemctl daemon-reload && sudo systemctl restart docker

# 预拉取镜像
docker pull python:3.11 && docker pull mongo:latest && docker pull mailhog/mailhog:latest

# 重启 Runner
sudo gitlab-runner restart
```

### Windows 快速修复

```powershell
# 1. 预拉取镜像
docker pull python:3.11 && docker pull mongo:latest && docker pull mailhog/mailhog:latest

# 2. 重启 Runner
cd D:\dev
.\gitlab-runner.exe restart
```

---

## 📚 参考资料

- [阿里云镜像加速器](https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors)
- [Docker 官方文档 - Daemon 配置](https://docs.docker.com/config/daemon/)
- [GitLab Runner 配置文档](https://docs.gitlab.com/runner/configuration/)
- [GitLab CI/CD 变量参考](https://docs.gitlab.com/ee/ci/variables/)
