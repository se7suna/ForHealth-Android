# 📧 SMTP 邮件服务器部署指南

## 📋 概述

本指南介绍如何在 For Health 项目中部署真实可用的 SMTP 邮件服务器，实现向用户真实邮箱发送验证码和通知邮件。

---

## 🎯 方案选择

### 方案一：Docker Postfix（推荐，已配置）

**优点：**
- ✅ 容器化部署，隔离性好
- ✅ 配置简单，无需修改系统
- ✅ 可以直接发送邮件到公网
- ✅ 支持中继模式（可选）

**缺点：**
- ⚠️ 需要配置 DNS 和 SPF 记录避免进入垃圾邮件
- ⚠️ IP 可能被部分邮件服务商拦截

---

## 🚀 部署步骤（使用 Docker Postfix）

### 1️⃣ 服务器环境准备

#### 检查端口占用
```bash
# 检查 25 端口是否被占用
sudo netstat -tlnp | grep :25

# 如果被占用，停止占用服务（通常是系统自带的 postfix）
sudo systemctl stop postfix
sudo systemctl disable postfix
```

#### 开放防火墙端口
```bash
# 允许 SMTP 端口 25
sudo firewall-cmd --permanent --add-port=25/tcp
sudo firewall-cmd --reload

# 或者使用 ufw（Ubuntu/Debian）
sudo ufw allow 25/tcp
```

#### 检查云服务商安全组
确保云服务器的安全组规则允许：
- **出站规则**：允许 TCP 25 端口（发送邮件到外部）
- **入站规则**：允许 TCP 25 端口（可选，仅当需要接收邮件时）

---

### 2️⃣ 上传修改后的文件到服务器

#### 方式一：使用 Git
```bash
# 在本地提交更改
git add docker-compose.prod.yml backend/.env.production backend/app/utils/email.py
git commit -m "feat: 配置 Postfix SMTP 邮件服务器"
git push origin develop

# 在服务器上拉取
cd /path/to/for_health
git pull origin develop
```

#### 方式二：手动上传
```bash
# 从本地上传文件到服务器
scp docker-compose.prod.yml user@124.70.161.90:/path/to/for_health/
scp backend/.env.production user@124.70.161.90:/path/to/for_health/backend/
scp backend/app/utils/email.py user@124.70.161.90:/path/to/for_health/backend/app/utils/
```

---

### 3️⃣ 配置 DNS（重要！避免进垃圾邮件）

#### A 记录配置
在你的域名 DNS 管理后台添加：
```
类型: A
主机记录: mail
记录值: 124.70.161.90
TTL: 600
```

#### PTR 反向解析（联系云服务商配置）
将 IP `124.70.161.90` 反向解析到 `mail.forhealth.com`

#### SPF 记录配置
```
类型: TXT
主机记录: @
记录值: v=spf1 ip4:124.70.161.90 ~all
TTL: 600
```

#### DKIM 配置（可选，增强信誉）
```bash
# 进入 Postfix 容器
docker exec -it for_health_postfix sh

# 生成 DKIM 密钥
opendkim-genkey -t -s mail -d forhealth.com

# 查看公钥并添加到 DNS TXT 记录
cat mail.txt
```

---

### 4️⃣ 修改 docker-compose.prod.yml 配置

编辑服务器上的配置文件：
```bash
cd /path/to/for_health
nano docker-compose.prod.yml
```

**关键配置：**
```yaml
  postfix:
    image: mwader/postfix-relay
    container_name: for_health_postfix
    restart: always
    ports:
      - "25:25"
    networks:
      - for_health_network
    environment:
      # 修改为你的域名（必须）
      - POSTFIX_myhostname=mail.forhealth.com
      - POSTFIX_mynetworks=127.0.0.0/8 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16

      # 如果直接发送失败，可以使用中继模式（推荐）
      # 使用 Gmail 作为中继示例：
      # - POSTFIX_relayhost=[smtp.gmail.com]:587
      # - POSTFIX_relayhost_username=your_email@gmail.com
      # - POSTFIX_relayhost_password=your_app_password
```

---

### 5️⃣ 启动服务

```bash
cd /path/to/for_health

# 停止旧服务
docker-compose -f docker-compose.prod.yml down

# 重新构建并启动
docker-compose -f docker-compose.prod.yml up -d --build

# 查看日志
docker logs for_health_postfix -f
docker logs for_health_backend -f
```

---

### 6️⃣ 测试邮件发送

#### 方法一：通过应用接口测试
```bash
# 使用 API 发送注册验证码
curl -X POST http://124.70.161.90:8000/api/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d '{"email": "your_real_email@gmail.com"}'
```

#### 方法二：进入容器直接测试
```bash
# 进入 backend 容器
docker exec -it for_health_backend bash

# 运行 Python 测试脚本
python3 << 'EOF'
import asyncio
from app.utils.email import send_email

async def test():
    result = await send_email(
        to_email="your_real_email@gmail.com",
        subject="测试邮件",
        body="<h1>这是来自 For Health 的测试邮件</h1>"
    )
    print(f"发送结果: {result}")

asyncio.run(test())
EOF
```

#### 方法三：查看 Postfix 队列
```bash
# 查看邮件队列状态
docker exec for_health_postfix postqueue -p

# 查看 Postfix 日志
docker exec for_health_postfix tail -f /var/log/mail.log
```

---

## ⚠️ 常见问题和解决方案

### 问题 1：邮件进入垃圾邮件箱

**原因：**
- 缺少 SPF/DKIM/DMARC 记录
- IP 信誉度低
- 发件域名与服务器不匹配

**解决方案：**
1. 配置完整的 DNS 记录（SPF、PTR）
2. 使用中继模式（通过 Gmail/SendGrid）
3. 预热 IP（逐步增加发送量）

### 问题 2：邮件发送失败（Connection refused）

**诊断命令：**
```bash
# 检查容器是否运行
docker ps | grep postfix

# 检查端口监听
docker exec for_health_postfix netstat -tlnp | grep 25

# 测试外部连接
telnet smtp.gmail.com 587
```

**解决方案：**
- 检查防火墙和安全组配置
- 确认云服务商未封禁 25 端口（某些云服务商默认封禁）

### 问题 3：云服务商封禁 25 端口

**症状：**
```
Connection timeout when connecting to external SMTP servers
```

**解决方案：**
使用中继模式通过 587/465 端口发送：
```yaml
environment:
  - POSTFIX_relayhost=[smtp.gmail.com]:587
  - POSTFIX_relayhost_username=your_email@gmail.com
  - POSTFIX_relayhost_password=your_app_password
```

---

## 🔄 中继模式配置（推荐）

如果直接发送邮件被拦截率高，使用可信的 SMTP 中继服务：

### 使用 Gmail 中继
```yaml
environment:
  - POSTFIX_myhostname=mail.forhealth.com
  - POSTFIX_relayhost=[smtp.gmail.com]:587
  - POSTFIX_relayhost_username=your_email@gmail.com
  - POSTFIX_relayhost_password=abcd efgh ijkl mnop  # 应用专用密码
```

### 使用阿里云企业邮箱中继
```yaml
environment:
  - POSTFIX_myhostname=mail.forhealth.com
  - POSTFIX_relayhost=[smtpdm.aliyun.com]:465
  - POSTFIX_relayhost_username=noreply@yourdomain.com
  - POSTFIX_relayhost_password=your_smtp_password
```

---

## 📊 监控和维护

### 查看邮件发送日志
```bash
docker logs for_health_postfix --tail 100 -f
```

### 清空邮件队列
```bash
docker exec for_health_postfix postsuper -d ALL
```

### 重启 Postfix 服务
```bash
docker restart for_health_postfix
```

---

## 🎯 生产环境检查清单

- [ ] DNS A 记录配置完成
- [ ] SPF 记录添加
- [ ] PTR 反向解析配置（联系云服务商）
- [ ] 防火墙 25 端口开放
- [ ] 云安全组出站规则配置
- [ ] docker-compose.prod.yml 配置更新
- [ ] .env.production 配置更新
- [ ] 容器成功启动
- [ ] 测试邮件发送成功
- [ ] 检查邮件未进垃圾箱
- [ ] 配置监控和日志记录

---

## 📚 参考资料

- [Postfix 官方文档](http://www.postfix.org/documentation.html)
- [mwader/postfix-relay 镜像文档](https://github.com/mwader/postfix-relay)
- [SPF 记录配置指南](https://www.spf-record.com/)
- [DKIM 配置教程](https://www.dkim.org/)

---

## 💡 建议

对于生产环境，推荐使用以下方案之一：

1. **Postfix + Gmail 中继**（免费，每天500封）
2. **SendGrid**（免费额度：每天100封）
3. **阿里云邮件推送**（国内稳定）
4. **AWS SES**（成本低，可靠性高）

自建 Postfix 适合学习和小规模应用，大规模应用建议使用专业邮件服务。
