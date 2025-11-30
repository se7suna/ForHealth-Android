# Refresh Token 使用指南

## 概述

我们实现了 Refresh Token 机制，让用户登录一次后长期保持登录状态，无需频繁重新输入密码。

## Token 类型

### 1. Access Token (短期有效)
- **有效期**: 15 分钟
- **用途**: 访问受保护的 API 端点
- **存储**: 内存或 sessionStorage（推荐）

### 2. Refresh Token (长期有效)
- **有效期**: 30 天
- **用途**: 刷新过期的 access token
- **存储**: localStorage（持久化）

## API 端点

### 1. 登录 - 获取 Tokens

**端点**: `POST /api/auth/login`

**请求**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**响应**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### 2. 刷新 Token

**端点**: `POST /api/auth/refresh`

**请求**:
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // 新的 access token
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // 新的 refresh token
  "token_type": "bearer"
}
```

## 前端集成方案

### 1. Token 存储

```javascript
// 登录成功后保存 tokens
function saveTokens(accessToken, refreshToken) {
  // Access token 存储在内存或 sessionStorage（临时）
  sessionStorage.setItem('access_token', accessToken);

  // Refresh token 存储在 localStorage（持久）
  localStorage.setItem('refresh_token', refreshToken);
}

// 读取 tokens
function getAccessToken() {
  return sessionStorage.getItem('access_token');
}

function getRefreshToken() {
  return localStorage.getItem('refresh_token');
}

// 清除 tokens（登出）
function clearTokens() {
  sessionStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
}
```

### 2. HTTP 拦截器（自动刷新 Token）

使用 Axios 示例：

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8000'
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

// 请求拦截器 - 添加 access token
api.interceptors.request.use(
  config => {
    const token = getAccessToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// 响应拦截器 - 处理 401 错误，自动刷新 token
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // 如果是 401 错误且没有重试过
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新，将请求加入队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = `Bearer ${token}`;
          return api(originalRequest);
        }).catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = getRefreshToken();

      if (!refreshToken) {
        // 没有 refresh token，跳转到登录页
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        // 调用刷新 token 接口
        const response = await axios.post('http://localhost:8000/api/auth/refresh', {
          refresh_token: refreshToken
        });

        const { access_token, refresh_token: newRefreshToken } = response.data;

        // 保存新的 tokens
        saveTokens(access_token, newRefreshToken);

        // 更新原请求的 token
        originalRequest.headers['Authorization'] = `Bearer ${access_token}`;

        // 处理队列中的请求
        processQueue(null, access_token);

        // 重试原请求
        return api(originalRequest);
      } catch (refreshError) {
        // 刷新失败，清除 tokens 并跳转登录
        processQueue(refreshError, null);
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

### 3. React Hook 示例

```javascript
import { useState, useEffect } from 'react';
import api from './api'; // 上面配置的 axios 实例

export function useAuth() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    // 检查是否有 refresh token
    const refreshToken = getRefreshToken();
    setIsAuthenticated(!!refreshToken);

    // 如果有 token，获取用户信息
    if (refreshToken) {
      fetchUserProfile();
    }
  }, []);

  const login = async (email, password) => {
    try {
      const response = await axios.post('/api/auth/login', { email, password });
      const { access_token, refresh_token } = response.data;

      saveTokens(access_token, refresh_token);
      setIsAuthenticated(true);

      await fetchUserProfile();
      return true;
    } catch (error) {
      console.error('登录失败:', error);
      return false;
    }
  };

  const logout = () => {
    clearTokens();
    setIsAuthenticated(false);
    setUser(null);
  };

  const fetchUserProfile = async () => {
    try {
      const response = await api.get('/api/user/profile');
      setUser(response.data);
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  return { isAuthenticated, user, login, logout };
}
```

## 工作流程

```
用户登录
    ↓
获得 access_token (15分钟) + refresh_token (30天)
    ↓
使用 access_token 访问 API
    ↓
access_token 过期 (15分钟后)
    ↓
API 返回 401 错误
    ↓
前端自动使用 refresh_token 刷新
    ↓
获得新的 access_token + refresh_token
    ↓
重试原请求
    ↓
继续使用... (循环)
    ↓
30天后 refresh_token 也过期
    ↓
跳转到登录页
```

## 安全性说明

1. **Access Token 短期有效**：即使被窃取，15分钟后自动失效
2. **Refresh Token 长期有效但只能刷新**：不能直接访问 API
3. **每次刷新都返回新的 tokens**：防止 token 重放攻击
4. **建议实现登出所有设备功能**：通过后端维护 token 黑名单

## 配置说明

可以通过环境变量调整 token 有效期：

```env
# .env 文件
ACCESS_TOKEN_EXPIRE_MINUTES=15  # Access token 有效期（分钟）
REFRESH_TOKEN_EXPIRE_DAYS=30    # Refresh token 有效期（天）
```

## 测试

### 1. 测试登录
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@user.com","password":"test1234"}'
```

### 2. 测试刷新
```bash
curl -X POST http://localhost:8000/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refresh_token":"YOUR_REFRESH_TOKEN_HERE"}'
```

### 3. 测试受保护的端点
```bash
curl -X GET http://localhost:8000/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```
