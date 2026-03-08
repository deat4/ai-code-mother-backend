# 后端更新 - 前端对接指南

## 更新日期
2026-03-08

---

## 一、新功能概览

### 1. 对话历史管理 ✅
- 用户消息和 AI 消息持久化存储
- 支持游标分页加载历史对话
- 按应用隔离对话历史

### 2. 应用版本管理 ✅
- AI 每次生成自动创建版本
- 支持版本对比（差异高亮）
- 支持版本回退

### 3. AI 生成中断 ✅
- 流式生成过程中可随时停止
- 通过 sessionId 管理生成会话

### 4. 环境变量配置 ✅
- 部署域名和预览域名可配置
- 前端需配置对应环境变量

### 5. 对话记忆功能 ✅
- 每个应用独立的对话上下文
- AI 能记住之前的对话内容

---

## 二、API 接口变更

### 2.1 对话历史 API

#### 获取应用对话历史（游标分页）
```http
GET /api/chatHistory/app/{appId}?pageSize=10&lastCreateTime=2026-03-08T10:00:00
```

**参数说明**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用ID |
| pageSize | int | 否 | 每页大小，默认10 |
| lastCreateTime | DateTime | 否 | 游标时间（最后一条消息的创建时间） |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1,
        "appId": 123,
        "message": "帮我写一个登录页面",
        "messageType": "user",
        "createTime": "2026-03-08T10:00:00"
      },
      {
        "id": 2,
        "appId": 123,
        "message": "好的，我已经为您生成了...",
        "messageType": "ai",
        "createTime": "2026-03-08T10:01:00"
      }
    ],
    "totalRow": 20,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

**使用场景**:
- 进入应用页面时加载历史对话
- 上拉加载更多历史消息

---

### 2.2 应用版本 API

#### 获取版本列表
```http
POST /api/app/version/list/page
Content-Type: application/json

{
  "appId": 123,
  "current": 1,
  "pageSize": 10
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1,
        "appId": 123,
        "versionNumber": 1,
        "versionName": "v1",
        "summary": "AI 生成：帮我写一个登录页面...",
        "changeType": "CREATE",
        "diffSummary": "+50 -0 ~0 行",
        "createdAt": "2026-03-08T10:00:00",
        "isCurrent": true
      }
    ],
    "totalRow": 5
  }
}
```

#### 获取版本详情
```http
GET /api/app/version/get/detail?versionId=1
```

#### 对比两个版本
```http
GET /api/app/version/diff?appId=123&oldVersion=1&newVersion=2
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "oldVersion": 1,
    "newVersion": 2,
    "oldContent": "...",
    "newContent": "...",
    "diffHtml": "<div class='diff-added'>+ 新增内容</div><div class='diff-removed'>- 删除内容</div>",
    "stats": {
      "additions": 10,
      "deletions": 5,
      "changes": 3,
      "totalLines": 100
    }
  }
}
```

#### 版本回退
```http
POST /api/app/version/rollback
Content-Type: application/json

{
  "appId": 123,
  "targetVersion": 1
}
```

---

### 2.3 AI 生成中断 API

#### SSE 流式生成（已更新）
```http
GET /api/app/chat/gen/code?appId=123&message=帮我写一个登录页面
```

**SSE 事件流**:
```
event: session
data: {"sessionId":"abc123def456"}

data: {"d":"好的"}
data: {"d":"，我"}
data: {"d":"来帮您"}

event: done
data:
```

**关键变更**: 
- 第一个事件是 `session`，返回 `sessionId`
- 前端需要保存 `sessionId` 用于停止生成

#### 停止 AI 生成
```http
POST /api/app/chat/stop?sessionId=abc123def456
```

**响应示例**:
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

## 三、前端任务清单

### 3.1 对话历史功能

#### 任务 1：加载历史对话
```javascript
// 进入应用页面时，加载历史对话
async function loadChatHistory(appId) {
  const response = await fetch(`/api/chatHistory/app/${appId}?pageSize=10`);
  const result = await response.json();
  
  if (result.code === 0 && result.data.records.length > 0) {
    // 有历史记录，显示对话
    displayMessages(result.data.records);
  } else {
    // 没有历史记录，自动发送初始化提示词
    sendInitPrompt(appId);
  }
}
```

#### 任务 2：上拉加载更多
```javascript
// 记录最后一条消息的时间
let lastCreateTime = null;

async function loadMoreHistory(appId) {
  const params = new URLSearchParams({
    pageSize: 10,
    ...(lastCreateTime && { lastCreateTime })
  });
  
  const response = await fetch(`/api/chatHistory/app/${appId}?${params}`);
  const result = await response.json();
  
  if (result.data.records.length > 0) {
    // 更新 lastCreateTime
    lastCreateTime = result.data.records[result.data.records.length - 1].createTime;
    prependMessages(result.data.records);
  }
}
```

---

### 3.2 AI 生成中断功能

#### 任务 3：保存 sessionId 并显示停止按钮
```javascript
let currentSessionId = null;
let eventSource = null;

function startGeneration(appId, message) {
  eventSource = new EventSource(`/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`);
  
  // 接收 sessionId
  eventSource.addEventListener('session', (event) => {
    const data = JSON.parse(event.data);
    currentSessionId = data.sessionId;
    showStopButton(); // 显示停止按钮
  });
  
  // 接收内容
  eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    appendContent(data.d);
  };
  
  // 生成完成
  eventSource.addEventListener('done', () => {
    eventSource.close();
    hideStopButton();
    currentSessionId = null;
  });
  
  // 错误处理
  eventSource.onerror = () => {
    eventSource.close();
    hideStopButton();
    currentSessionId = null;
  };
}
```

#### 任务 4：停止生成
```javascript
async function stopGeneration() {
  if (!currentSessionId) return;
  
  try {
    await fetch(`/api/app/chat/stop?sessionId=${currentSessionId}`, {
      method: 'POST'
    });
  } finally {
    eventSource?.close();
    hideStopButton();
    currentSessionId = null;
  }
}
```

---

### 3.3 版本管理功能

#### 任务 5：版本历史弹窗
```javascript
// UI 组件建议
<Modal title="版本历史">
  <VersionList>
    {versions.map(v => (
      <VersionItem 
        key={v.id}
        version={v}
        isCurrent={v.isCurrent}
        onViewDetail={() => showVersionDetail(v.id)}
        onCompare={() => showDiffModal(v.versionNumber)}
        onRollback={() => confirmRollback(v.versionNumber)}
      />
    ))}
  </VersionList>
</Modal>
```

#### 任务 6：版本对比组件
```html
<!-- 差异展示样式 -->
<style>
.diff-added {
  background-color: #e6ffec;
  color: #22863a;
}
.diff-removed {
  background-color: #ffebe9;
  color: #cb2431;
  text-decoration: line-through;
}
.diff-line {
  padding: 2px 8px;
  font-family: monospace;
}
</style>

<div v-html="diffResult.diffHtml"></div>
```

---

## 四、环境变量配置

### 4.1 .env 文件配置

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8123/api
VITE_DEPLOY_DOMAIN=http://localhost:8123/api/static
VITE_PREVIEW_DOMAIN=http://localhost:8123/api/preview

# .env.production
VITE_API_BASE_URL=https://api.yoursite.com/api
VITE_DEPLOY_DOMAIN=https://apps.yoursite.com
VITE_PREVIEW_DOMAIN=https://preview.yoursite.com
```

### 4.2 使用示例

```javascript
// 构建部署 URL
const deployUrl = `${import.meta.env.VITE_DEPLOY_DOMAIN}/${deployKey}/`;

// 构建预览 URL
const previewUrl = `${import.meta.env.VITE_PREVIEW_DOMAIN}/${codeGenType}_${appId}/`;
```

---

## 五、对话记忆功能（前端无需改动）

### 说明
- 后端已实现对话记忆，AI 会记住之前的对话内容
- 每个应用的对话上下文完全隔离
- 对话历史存储在 Redis 中，最多保留 20 条消息

### 行为变化
- **之前**: 每次 AI 生成都是全新的，没有上下文
- **现在**: AI 会记住之前的对话，支持多轮对话迭代

### 示例
```
用户：帮我写一个登录页面
AI：好的，已为您生成登录页面...

用户（第二次）：把背景改成蓝色
AI：好的，我在之前的登录页面上把背景改成了蓝色...
```

---

## 六、消息类型枚举

```javascript
const MessageType = {
  USER: 'user',  // 用户消息
  AI: 'ai'       // AI 消息
};
```

---

## 七、变更类型枚举

```javascript
const ChangeType = {
  CREATE: 'CREATE',      // 首次创建
  UPDATE: 'UPDATE',      // 更新
  ROLLBACK: 'ROLLBACK'   // 回退
};
```

---

## 八、注意事项

### 8.1 对话历史加载时机
- 进入应用页面时**先加载历史对话**
- 如果有历史记录，直接展示
- 如果没有历史记录，才自动发送初始化提示词

### 8.2 SSE 事件顺序
1. `event: session` → 保存 sessionId
2. `data: {"d":"..."}` → 接收内容流
3. `event: done` → 生成完成

### 8.3 版本回退
- 回退会创建新版本，不会删除历史版本
- 回退后版本号会继续递增

### 8.4 错误处理
- 所有接口统一返回格式：`{ code, data, message }`
- `code === 0` 表示成功
- 其他 code 表示失败，message 包含错误信息

---

## 九、联调时间

建议联调时间：**2026-03-09**

如有问题，请联系后端开发。

---

**文档版本**: v1.0  
**创建时间**: 2026-03-08