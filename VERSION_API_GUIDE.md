# 应用版本管理 API 文档

## 概述

版本管理功能允许用户追踪应用的迭代历史、查看版本差异、回退到历史版本。

**核心特性**：
- AI 每次生成代码自动创建版本
- 版本差异可视化（新增/删除/修改行数统计）
- 支持回退到任意历史版本
- 每个应用独立管理版本（版本号从 1 开始）

---

## 一、API 接口列表

| 方法 | 端点 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/app/version/list/page` | 分页查询版本列表 | 应用所有者/管理员 |
| GET | `/api/app/version/detail` | 获取版本详情 | 应用所有者/管理员 |
| POST | `/api/app/version/create` | 创建新版本 | 应用所有者 |
| GET | `/api/app/version/diff` | 对比两个版本 | 应用所有者/管理员 |
| POST | `/api/app/version/rollback` | 回退到指定版本 | 应用所有者 |
| GET | `/api/app/version/current/content` | 获取当前版本内容 | 应用所有者/管理员 |
| DELETE | `/api/app/version/admin/{versionId}` | 管理员删除版本 | 管理员 |

---

## 二、API 详细说明

### 2.1 分页查询版本列表

**接口**：`POST /api/app/version/list/page`

**请求参数**：
```json
{
  "appId": 123,
  "current": 1,
  "pageSize": 10,
  "versionNumber": null  // 可选：按版本号过滤
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |
| current | int | 否 | 当前页码，默认 1 |
| pageSize | int | 否 | 每页大小，默认 10 |
| versionNumber | Integer | 否 | 按版本号过滤（可选） |

**响应示例**：
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
        "createdBy": 456,
        "creatorName": "张三",
        "isCurrent": true
      },
      {
        "id": 2,
        "appId": 123,
        "versionNumber": 2,
        "versionName": "v2",
        "summary": "AI 生成：把背景改成蓝色...",
        "changeType": "UPDATE",
        "diffSummary": "+10 -5 ~3 行",
        "createdAt": "2026-03-08T11:00:00",
        "createdBy": 456,
        "creatorName": "张三",
        "isCurrent": false
      }
    ],
    "totalRow": 5,
    "pageNum": 1,
    "pageSize": 10
  },
  "message": "ok"
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 版本 ID |
| appId | Long | 应用 ID |
| versionNumber | Integer | 版本号（从 1 开始） |
| versionName | String | 版本名称（如 "v1"） |
| summary | String | 版本摘要/变更说明 |
| changeType | String | 变更类型：CREATE/UPDATE/ROLLBACK |
| diffSummary | String | 与上一版本的差异摘要（如 "+10 -5 ~3 行"） |
| createdAt | DateTime | 创建时间 |
| createdBy | Long | 创建者用户 ID |
| creatorName | String | 创建者姓名 |
| isCurrent | Boolean | 是否为当前版本 |

---

### 2.2 获取版本详情

**接口**：`GET /api/app/version/detail?versionId=1`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| versionId | Long | 是 | 版本 ID |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "appId": 123,
    "versionNumber": 1,
    "versionName": "v1",
    "summary": "AI 生成：帮我写一个登录页面...",
    "changeType": "CREATE",
    "diffSummary": "+50 -0 ~0 行",
    "createdAt": "2026-03-08T10:00:00",
    "createdBy": 456,
    "creatorName": "张三",
    "isCurrent": true,
    
    // 以下是详情特有字段
    "content": "<html>...</html>",
    "creator": {
      "id": 456,
      "userName": "张三",
      "userAccount": "zhangsan"
    },
    "canRollback": true,
    "prevVersion": null,
    "nextVersion": 2
  },
  "message": "ok"
}
```

**详情特有字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| content | String | 完整代码内容 |
| creator | UserVO | 创建者信息 |
| canRollback | Boolean | 是否可回退（当前版本不可回退） |
| prevVersion | Integer | 上一个版本号（第一个版本为 null） |
| nextVersion | Integer | 下一个版本号（最后一个版本为 null） |

---

### 2.3 创建新版本

**接口**：`POST /api/app/version/create`

**请求参数**：
```json
{
  "appId": 123,
  "content": "<html>...</html>",
  "versionName": "v3",        // 可选，不传自动生成
  "summary": "修复登录按钮样式",
  "changeType": "UPDATE"
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |
| content | String | 是 | 代码内容 |
| versionName | String | 否 | 版本名称（不传自动生成 "v{版本号}"） |
| summary | String | 否 | 变更说明 |
| changeType | String | 是 | 变更类型：CREATE/UPDATE/ROLLBACK |
| parentVersion | Integer | 否 | 父版本号（回退时使用） |

**响应示例**：
```json
{
  "code": 0,
  "data": 3,  // 新创建的版本 ID
  "message": "ok"
}
```

**注意**：
- 通常由后端 AI 生成后自动调用，前端一般不需要主动调用
- 版本号自动递增

---

### 2.4 对比两个版本

**接口**：`GET /api/app/version/diff?appId=123&oldVersion=1&newVersion=2`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |
| oldVersion | Integer | 是 | 旧版本号 |
| newVersion | Integer | 是 | 新版本号 |

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "oldVersion": 1,
    "newVersion": 2,
    "oldContent": "<html>...</html>",
    "newContent": "<html>...</html>",
    "diffHtml": "<div class='diff-removed'>- body { background: white; }</div><div class='diff-added'>+ body { background: blue; }</div>",
    "stats": {
      "additions": 10,
      "deletions": 5,
      "changes": 3,
      "totalLines": 100
    }
  },
  "message": "ok"
}
```

**差异统计字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| additions | Integer | 新增行数 |
| deletions | Integer | 删除行数 |
| changes | Integer | 修改行数 |
| totalLines | Integer | 新版本总行数 |

**diffHtml 说明**：
- 返回 HTML 格式的差异标记
- 删除的行：`<div class='diff-removed'>- 内容</div>`
- 新增的行：`<div class='diff-added'>+ 内容</div>`
- 未变更的行：`<div class='diff-line'>内容</div>`

**前端样式建议**：
```css
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
```

---

### 2.5 版本回退

**接口**：`POST /api/app/version/rollback`

**请求参数**：
```json
{
  "appId": 123,
  "targetVersion": 1
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |
| targetVersion | Integer | 是 | 目标版本号 |

**响应示例**：
```json
{
  "code": 0,
  "data": 6,  // 新创建的版本 ID（回退版本）
  "message": "ok"
}
```

**回退逻辑**：
1. 验证目标版本是否存在
2. 创建新版本（changeType = ROLLBACK）
3. 复制目标版本的代码内容
4. 版本号继续递增（不会覆盖历史版本）

**示例**：
- 当前版本：v5
- 回退到：v2
- 结果：创建 v6（内容与 v2 相同）

---

### 2.6 获取当前版本内容

**接口**：`GET /api/app/version/current/content?appId=123`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |

**响应示例**：
```json
{
  "code": 0,
  "data": "<html>...</html>",
  "message": "ok"
}
```

---

### 2.7 管理员删除版本

**接口**：`DELETE /api/app/version/admin/{versionId}`

**权限**：仅管理员

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| versionId | Long | 是 | 版本 ID |

**响应示例**：
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

## 三、数据结构

### 3.1 变更类型枚举 (ChangeType)

```javascript
const ChangeType = {
  CREATE: 'CREATE',      // 首次创建
  UPDATE: 'UPDATE',      // 更新
  ROLLBACK: 'ROLLBACK'   // 回退
};
```

### 3.2 AppVersionVO - 版本列表视图

```typescript
interface AppVersionVO {
  id: number;                  // 版本 ID
  appId: number;               // 应用 ID
  versionNumber: number;       // 版本号（从 1 开始）
  versionName: string;         // 版本名称（如 "v1"）
  summary: string;             // 版本摘要
  changeType: 'CREATE' | 'UPDATE' | 'ROLLBACK';  // 变更类型
  diffSummary: string;         // 差异摘要（如 "+10 -5 ~3 行"）
  createdAt: string;           // 创建时间
  createdBy: number;           // 创建者用户 ID
  creatorName: string;         // 创建者姓名
  isCurrent: boolean;          // 是否为当前版本
}
```

### 3.3 AppVersionDetailVO - 版本详情视图

```typescript
interface AppVersionDetailVO extends AppVersionVO {
  content: string;             // 完整代码内容
  creator: UserVO;             // 创建者信息
  canRollback: boolean;        // 是否可回退
  prevVersion: number | null;  // 上一个版本号
  nextVersion: number | null;  // 下一个版本号
}
```

### 3.4 VersionDiffVO - 版本差异视图

```typescript
interface VersionDiffVO {
  oldVersion: number;          // 旧版本号
  newVersion: number;          // 新版本号
  oldContent: string;          // 旧版本内容
  newContent: string;          // 新版本内容
  diffHtml: string;            // HTML 格式的差异
  stats: DiffStats;            // 差异统计
}

interface DiffStats {
  additions: number;           // 新增行数
  deletions: number;           // 删除行数
  changes: number;             // 修改行数
  totalLines: number;          // 总行数
}
```

---

## 四、使用场景

### 4.1 查看版本历史

```javascript
// 进入应用详情页，加载版本历史
async function loadVersionHistory(appId) {
  const response = await fetch('/api/app/version/list/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ appId, current: 1, pageSize: 10 })
  });
  
  const result = await response.json();
  if (result.code === 0) {
    displayVersionList(result.data.records);
    displayTotalCount(result.data.totalRow);
  }
}
```

### 4.2 查看版本详情

```javascript
async function showVersionDetail(versionId) {
  const response = await fetch(`/api/app/version/detail?versionId=${versionId}`);
  const result = await response.json();
  
  if (result.code === 0) {
    const detail = result.data;
    showCodeContent(detail.content);
    showNavigation(detail.prevVersion, detail.nextVersion);
    
    if (detail.canRollback) {
      showRollbackButton(detail.versionNumber);
    }
  }
}
```

### 4.3 版本对比

```javascript
async function compareVersions(appId, oldVersion, newVersion) {
  const response = await fetch(
    `/api/app/version/diff?appId=${appId}&oldVersion=${oldVersion}&newVersion=${newVersion}`
  );
  
  const result = await response.json();
  if (result.code === 0) {
    const diff = result.data;
    
    // 显示差异统计
    showDiffStats(diff.stats);
    
    // 显示差异内容（HTML）
    document.getElementById('diff-container').innerHTML = diff.diffHtml;
  }
}
```

### 4.4 版本回退

```javascript
async function rollbackVersion(appId, targetVersion) {
  const confirmed = confirm(`确认回退到版本 v${targetVersion} 吗？`);
  if (!confirmed) return;
  
  const response = await fetch('/api/app/version/rollback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ appId, targetVersion })
  });
  
  const result = await response.json();
  if (result.code === 0) {
    alert('回退成功！新版本号：v' + (await getLatestVersion(appId)));
    reloadVersionHistory();
  }
}
```

---

## 五、前端 UI 建议

### 5.1 版本历史弹窗

```
┌─────────────────────────────────────────┐
│  版本历史                        [×]    │
├─────────────────────────────────────────┤
│  当前版本：v5    总版本数：5            │
├─────────────────────────────────────────┤
│  v5 (当前)  2026-03-08 11:00           │
│  AI 生成：把背景改成蓝色                │
│  +10 -5 ~3 行     [查看] [对比]         │
├─────────────────────────────────────────┤
│  v4         2026-03-08 10:30           │
│  AI 生成：添加登录表单验证              │
│  +20 -0 ~0 行     [查看] [对比] [回退]   │
├─────────────────────────────────────────┤
│  v3         2026-03-08 10:00           │
│  AI 生成：添加样式                      │
│  +15 -0 ~0 行     [查看] [对比] [回退]   │
└─────────────────────────────────────────┘
```

### 5.2 版本对比弹窗

```
┌─────────────────────────────────────────┐
│  版本对比：v1 → v2               [×]    │
├─────────────────────────────────────────┤
│  统计：+10 新增  -5 删除  ~3 修改       │
├─────────────────────────────────────────┤
│  - body { background: white; }          │
│  + body { background: blue; }           │
│                                         │
│    .login-form {                        │
│      width: 300px;                      │
│    }                                    │
└─────────────────────────────────────────┘
```

### 5.3 回退确认对话框

```
┌─────────────────────────────────────────┐
│  确认回退                               │
├─────────────────────────────────────────┤
│  确认回退到版本 v2 吗？                 │
│                                         │
│  回退后将创建新版本 v6，                │
│  当前版本 v5 的变更将保留在历史中。     │
│                                         │
│         [取消]    [确认回退]            │
└─────────────────────────────────────────┘
```

---

## 六、注意事项

### 6.1 版本号规则
- 每个应用独立管理版本号
- 版本号从 1 开始递增
- 回退操作也会创建新版本（版本号继续递增）

### 6.2 当前版本标识
- `isCurrent = true` 表示当前正在使用的版本
- 只有一个版本会被标记为当前版本
- 回退后会更新当前版本标识

### 6.3 版本内容
- `content` 字段存储完整代码内容
- 大文件（> 1MB）可能导致加载缓慢
- 建议使用懒加载，仅在查看详情时加载 `content`

### 6.4 权限控制
- 应用所有者：可以查看、创建、回退版本
- 管理员：可以查看、删除任意版本
- 非所有者：无权操作

### 6.5 差异计算
- 使用 java-diff-utils 计算
- 大文件对比可能耗时（建议显示加载状态）
- 差异结果已缓存，重复查询更快

---

## 七、错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40400 | 版本不存在 |
| 50000 | 系统内部错误 |
| 50001 | 操作失败 |

---

## 八、数据库表结构（参考）

```sql
CREATE TABLE `app_version` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `app_id` bigint NOT NULL,
  `version_number` int NOT NULL,
  `version_name` varchar(100),
  `content` longtext NOT NULL,
  `summary` varchar(500),
  `change_type` varchar(20) NOT NULL DEFAULT 'UPDATE',
  `diff_summary` varchar(500),
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `is_current` tinyint DEFAULT 0,
  `parent_version` int,
  
  UNIQUE KEY `uk_app_version` (`app_id`, `version_number`)
);
```

---

**文档版本**: v1.0  
**创建时间**: 2026-03-08  
**后端开发**: zkf