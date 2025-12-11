# 📧 快速部署 SMTP 邮件服务（无需域名版）

## 🎯 方案说明

使用 Postfix 直接从服务器发送邮件，**无需域名、无需 DNS 配置、无需第三方服务**。

### ⚠️ 注意事项
- ✅ 邮件可以正常发送
- ⚠️ 大部分邮件会进入**垃圾邮件箱**
- ⚠️ 部分邮件服务商可能直接拒收
- 💡 适合：测试环境、内部系统、课程项目

---

## 🚀 服务器部署步骤（3 步完成）

### 步骤 1：上传配置文件

**方式 A - 使用 Git（推荐）：**
```bash
# 本地提交更改
cd /path/to/for_health
git add docker-compose.prod.yml backend/.env.production backend/app/utils/email.py
git commit -m "feat: 配置 Postfix SMTP 邮件服务器（无域名直接发送）"
git push origin develop

# 服务器拉取
ssh user@124.70.161.90
cd /path/to/for_health
git pull origin develop
```

**方式 B - 手动上传：**
```bash
# 从本地上传 3 个文件到服务器
scp docker-compose.prod.yml user@124.70.161.90:/path/to/for_health/
scp backend/.env.production user@124.70.161.90:/path/to/for_health/backend/
scp backend/app/utils/email.py user@124.70.161.90:/path/to/for_health/backend/app/utils/
```

---

### 步骤 2：服务器环境准备

```bash
# SSH 登录服务器
ssh user@124.70.161.90

# 进入项目目录
cd /path/to/for_health

# 检查 25 端口是否被占用
sudo netstat -tlnp | grep :25

# 如果被占用（通常是系统自带的 postfix），停止它
sudo systemctl stop postfix
sudo systemctl disable postfix

# 开放防火墙 25 端口（如果有防火墙）
sudo firewall-cmd --permanent --add-port=25/tcp
sudo firewall-cmd --reload

# 或者使用 ufw（Ubuntu/Debian）
sudo ufw allow 25/tcp
```

---

### 步骤 3：启动服务

```bash
# 停止旧服务
docker-compose -f docker-compose.prod.yml down

# 启动新服务（包括 Postfix）
docker-compose -f docker-compose.prod.yml up -d

# 查看容器状态
docker ps | grep postfix

# 查看 Postfix 日志
docker logs for_health_postfix -f
```

---

## ✅ 测试邮件发送

### 方法 1：通过 API 测试

```bash
# 测试注册验证码发送（替换为你的邮箱）
curl -X POST http://124.70.161.90:8000/api/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d '{"email": "your_email@qq.com"}'

# 预期返回
# {"message": "验证码已发送"}
```

### 方法 2：进入容器测试

```bash
# 进入 backend 容器
docker exec -it for_health_backend bash

# 运行 Python 测试
python3 << 'EOF'
import asyncio
from app.utils.email import send_email

async def test():
    result = await send_email(
        to_email="your_email@qq.com",
        subject="For Health 测试邮件",
        body="<h1>这是一封测试邮件</h1><p>如果你收到这封邮件，说明 SMTP 服务器配置成功！</p>"
    )
    print(f"发送结果: {'成功' if result else '失败'}")

asyncio.run(test())
EOF

# 退出容器
exit
```

---

## 🔍 检查邮件

### 1. 检查收件箱
首先检查你的邮箱收件箱，看是否收到邮件。

### 2. 检查垃圾邮件箱 ⭐
**大概率邮件在这里！** 检查以下文件夹：
- Gmail: "垃圾邮件" / "Spam"
- QQ邮箱: "垃圾箱"
- 163邮箱: "垃圾邮件"
- Outlook: "垃圾邮件"

### 3. 如果完全收不到

查看 Postfix 日志：
```bash
docker logs for_health_postfix --tail 50
```

常见错误：
- `Connection refused`: 端口未开放或被防火墙拦截
- `Relay access denied`: 配置错误
- `Host or domain name not found`: DNS 查询失败（正常，会重试）

---

## 📊 故障排查

### 问题 1：容器启动失败

**症状：**
```bash
docker ps | grep postfix
# 没有输出
```

**解决：**
```bash
# 查看启动日志
docker logs for_health_postfix

# 检查端口占用
sudo netstat -tlnp | grep :25

# 停止占用的服务
sudo systemctl stop postfix
```

---

### 问题 2：邮件发送失败

**检查步骤：**

```bash
# 1. 查看 backend 日志
docker logs for_health_backend --tail 50

# 2. 查看 Postfix 日志
docker logs for_health_postfix --tail 50

# 3. 检查邮件队列
docker exec for_health_postfix postqueue -p

# 4. 测试容器间网络连通性
docker exec for_health_backend ping postfix -c 3
```

---

### 问题 3：云服务商封禁 25 端口

**症状：**
```
Connection timeout when sending to external mail servers
```

**检查方法：**
```bash
# 测试外部 SMTP 连接
telnet smtp.gmail.com 25
```

**解决方案：**
如果云服务商封禁了 25 端口，需要：
1. 提交工单申请解封（阿里云/腾讯云支持）
2. 或者改用中继模式（需要第三方 SMTP）

---

## 🎯 预期结果

### ✅ 成功的标志

1. **容器运行正常：**
```bash
docker ps | grep postfix
# 输出：for_health_postfix ... Up 5 minutes
```

2. **API 返回成功：**
```json
{"message": "验证码已发送"}
```

3. **Postfix 日志显示发送：**
```
status=sent (250 2.0.0 OK)
```

4. **邮箱收到邮件**（可能在垃圾箱）

---

## 📝 配置文件说明

### docker-compose.prod.yml
```yaml
postfix:
  image: mwader/postfix-relay
  container_name: for_health_postfix
  restart: always
  ports:
    - "25:25"  # SMTP 端口
  environment:
    - POSTFIX_myhostname=mail.forhealth.com  # 主机名（可随意设置）
    - POSTFIX_mynetworks=127.0.0.0/8 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16
```

### .env.production
```bash
SMTP_HOST=postfix      # Postfix 容器名
SMTP_PORT=25           # 标准 SMTP 端口
SMTP_USER=             # 无需认证
SMTP_PASSWORD=         # 无需认证
SMTP_FROM_EMAIL=noreply@forhealth.com
SMTP_FROM_NAME=For Health
```

---

## 💡 使用建议

### 给测试用户的说明

> "注册/重置密码时，验证码邮件可能会被放入垃圾邮件箱，请检查：
> - Gmail: 垃圾邮件文件夹
> - QQ/163: 垃圾箱
>
> 如果 5 分钟内未收到，请检查邮箱地址是否正确，或联系管理员。"

### 提升送达率的方法

如果需要提高送达率，可以考虑：

1. **购买域名**（¥10-30/年）+ 配置 SPF 记录 → 送达率提升到 70%
2. **使用 Gmail 中继**（免费 500 封/天）→ 送达率 99%
3. **使用专业邮件服务**（SendGrid/阿里云/AWS SES）→ 送达率 99.9%

---

## 📞 需要帮助？

遇到问题时，提供以下信息：

```bash
# 收集诊断信息
echo "=== 容器状态 ==="
docker ps | grep -E "postfix|backend"

echo "=== Postfix 日志 ==="
docker logs for_health_postfix --tail 20

echo "=== Backend 日志 ==="
docker logs for_health_backend --tail 20

echo "=== 邮件队列 ==="
docker exec for_health_postfix postqueue -p

echo "=== 端口监听 ==="
sudo netstat -tlnp | grep :25
```

---

## 🎓 总结

**当前方案特点：**
- ✅ 部署简单，无需域名
- ✅ 完全自主，不依赖第三方
- ✅ 成本为零
- ⚠️ 邮件可能进垃圾箱
- 💡 适合测试和学习

**如果是正式生产环境，建议升级到带域名的方案或使用邮件中继服务。**
