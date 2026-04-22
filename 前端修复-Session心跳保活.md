# SSE Session 问题解决方案（后端已修复）

## 问题确认

前端配置正确：
- `axios.defaults.withCredentials = true` ✓
- `fetch SSE credentials: 'include'` ✓

**问题根因**：后端 Spring Session 在 SSE 长连接的 async dispatch 完成时，尝试保存已标记为 "invalidated" 的 session，导致后续请求报错。

## 后端修复方案

后端已添加 `SseAsyncSessionFilter`，在 SSE async dispatch 时阻止 session 操作：

### 实现原理

1. 检测请求是否是 `DispatcherType.ASYNC`
2. 如果是 async dispatch，使用 `NoOpSessionWrapper` 包装 session
3. 该 wrapper 不执行任何实际的 session 写操作
4. 避免 Spring Session 尝试保存已失效的 session

### 关键代码位置

- `SseAsyncSessionFilter.java` - SSE async session 过滤器
- `SessionConfig.java` - 过滤器注册配置

## 前端无需修改

后端已修复此问题，前端现有的心跳保活机制可以继续使用（增强保活效果），但核心问题已在后端解决。

### 心跳保活（可选，增强效果）

前端可以继续使用心跳请求保持 session 活跃：

```typescript
// 在 SSE 连接期间发送心跳保活（可选）
const startSessionHeartbeat = () => {
  return setInterval(() => {
    request.get('/api/user/session/keepalive')
      .then(() => console.log('Session保活成功'))
      .catch(() => console.warn('Session保活失败'));
  }, 30000); // 30秒间隔
};

// SSE 完成后清除心跳
```

### 心跳 API

```
GET /api/user/session/keepalive
```

该接口仅触发 session attribute 读取，刷新 Redis TTL，返回轻量的成功响应。

## 测试验证

后端应用已启动成功，请在前端测试 SSE 流式生成功能，验证：
1. SSE 流生成过程中无 "Session was invalidated" 错误
2. SSE 完成后，后续请求正常工作（不退出登录）