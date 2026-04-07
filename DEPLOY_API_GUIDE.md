# 应用部署 API 文档

## 概述

应用部署功能允许用户将生成的代码部署到独立的访问地址，生成可分享的应用链接。

**核心特性**：
- 一键部署生成的代码
- 生成独立的访问 URL（基于 deployKey）
- 支持重复部署（更新部署内容）
- 静态资源访问（支持 HTML 和多文件应用）

---

## 一、部署流程

```
用户生成代码
    ↓
预览目录：tmp/code_output/{codeGenType}_{appId}/
    ↓
调用部署接口
    ↓
复制到部署目录：tmp/code_deploy/{deployKey}/
    ↓
返回访问 URL：{VITE_DEPLOY_DOMAIN}/{deployKey}/
```

---

## 二、API 接口列表

| 方法 | 端点 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/app/deploy` | 部署应用 | 应用所有者 |
| GET | `/api/static/{deployKey}/**` | 访问已部署应用 | 公开 |
| GET | `/api/preview/{codeGenType}_{appId}/**` | 预览应用 | 公开 |

---

## 三、API 详细说明

### 3.1 部署应用

**接口**：`POST /api/app/deploy`

**请求参数**：
```json
{
  "appId": 123
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appId | Long | 是 | 应用 ID |

**响应示例**：
```json
{
  "code": 0,
  "data": "http://localhost:8123/api/static/aB3xYz/",
  "message": "ok"
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码（0 表示成功） |
| data | String | 部署后的访问 URL |
| message | String | 响应消息 |

**业务逻辑**：

1. **参数校验**：验证 appId 和登录状态
2. **权限验证**：仅应用所有者可以部署
3. **生成 deployKey**：
   - 如果应用已有 deployKey，继续使用（更新部署）
   - 如果没有，生成 6 位随机字符串（大小写字母 + 数字）
4. **检查源代码**：验证预览目录是否存在代码
5. **复制文件**：从预览目录复制到部署目录
6. **更新数据库**：记录 deployKey 和部署时间
7. **返回 URL**：拼接部署域名和 deployKey

**错误情况**：

| 错误码 | 说明 |
|--------|------|
| 40000 | 应用 ID 无效 |
| 40100 | 用户未登录 |
| 40101 | 无权限部署该应用 |
| 40400 | 应用不存在 |
| 50000 | 应用代码不存在，请先生成代码 |
| 50001 | 部署失败 |

---

### 3.2 访问已部署应用

**接口**：`GET /api/static/{deployKey}/**`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| deployKey | String | 部署标识（6 位随机字符串） |

**示例**：
```
GET /api/static/aB3xYz/
GET /api/static/aB3xYz/index.html
GET /api/static/aB3xYz/styles/main.css
```

**说明**：
- 所有已部署的应用都可以通过此接口访问
- deployKey 是应用唯一标识
- 支持访问任意静态文件（HTML、CSS、JS、图片等）
- 无需登录即可访问

**目录映射**：
```
URL: /api/static/aB3xYz/index.html
物理路径: tmp/code_deploy/aB3xYz/index.html

URL: /api/static/aB3xYz/styles/main.css
物理路径: tmp/code_deploy/aB3xYz/styles/main.css
```

---

### 3.3 预览应用

**接口**：`GET /api/preview/{codeGenType}_{appId}/**`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| codeGenType | String | 代码生成类型（HTML/MULTI_FILE） |
| appId | Long | 应用 ID |

**示例**：
```
GET /api/preview/HTML_123/
GET /api/preview/MULTI_FILE_456/index.html
```

**说明**：
- 用于预览已生成但未部署的代码
- 无需登录即可访问
- 适合在部署前快速预览效果

**目录映射**：
```
URL: /api/preview/HTML_123/
物理路径: tmp/code_output/HTML_123/

URL: /api/preview/MULTI_FILE_456/index.html
物理路径: tmp/code_output/MULTI_FILE_456/index.html
```

---

## 四、前端集成

### 4.1 环境变量配置

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

### 4.2 部署应用

```javascript
async function deployApp(appId) {
  const confirmed = confirm('确认部署此应用吗？');
  if (!confirmed) return;
  
  try {
    const response = await fetch('/api/app/deploy', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({ appId })
    });
    
    const result = await response.json();
    
    if (result.code === 0) {
      const deployUrl = result.data;
      alert(`部署成功！访问地址：${deployUrl}`);
      
      // 更新应用信息
      updateAppInfo(appId, { deployed: true, deployUrl });
    } else {
      alert(`部署失败：${result.message}`);
    }
  } catch (error) {
    console.error('部署失败:', error);
    alert('部署失败，请稍后重试');
  }
}
```

### 4.3 访问已部署应用

```javascript
// 构建部署 URL
function getDeployUrl(deployKey) {
  const deployDomain = import.meta.env.VITE_DEPLOY_DOMAIN;
  return `${deployDomain}/${deployKey}/`;
}

// 在新窗口打开已部署应用
function openDeployedApp(deployKey) {
  const url = getDeployUrl(deployKey);
  window.open(url, '_blank');
}

// 在 iframe 中预览
function previewDeployedApp(deployKey) {
  const url = getDeployUrl(deployKey);
  document.getElementById('preview-iframe').src = url;
}
```

### 4.4 预览未部署应用

```javascript
// 构建预览 URL
function getPreviewUrl(codeGenType, appId) {
  const previewDomain = import.meta.env.VITE_PREVIEW_DOMAIN;
  return `${previewDomain}/${codeGenType}_${appId}/`;
}

// 预览生成的代码
function previewGeneratedCode(app) {
  const url = getPreviewUrl(app.codeGenType, app.id);
  window.open(url, '_blank');
}
```

---

## 五、前端 UI 建议

### 5.1 应用卡片

```
┌──────────────────────────────────────┐
│  应用名称                            │
│  [预览] [部署] [编辑] [删除]         │
│                                      │
│  状态：已部署                        │
│  访问地址：http://.../aB3xYz/        │
│  部署时间：2026-03-08 10:00          │
└──────────────────────────────────────┘
```

### 5.2 部署按钮状态

| 应用状态 | 按钮文本 | 按钮状态 |
|---------|---------|---------|
| 未生成代码 | "请先生成代码" | 禁用 |
| 已生成未部署 | "部署" | 启用 |
| 已部署 | "重新部署" | 启用 |

### 5.3 部署确认对话框

```
┌─────────────────────────────────────┐
│  确认部署                           │
├─────────────────────────────────────┤
│  部署后将生成可公开访问的链接。     │
│                                     │
│  如果之前已部署，将会更新部署内容。 │
│                                     │
│         [取消]    [确认部署]        │
└─────────────────────────────────────┘
```

### 5.4 部署成功提示

```
┌─────────────────────────────────────┐
│  部署成功！ ✓                       │
├─────────────────────────────────────┤
│  访问地址：                         │
│  http://.../aB3xYz/                 │
│                                     │
│  [复制链接] [立即访问] [关闭]       │
└─────────────────────────────────────┘
```

---

## 六、数据结构

### 6.1 AppVO - 应用视图（部署相关字段）

```typescript
interface AppVO {
  id: number;                    // 应用 ID
  appName: string;               // 应用名称
  codeGenType: string;           // 代码生成类型（HTML/MULTI_FILE）
  deployKey: string | null;      // 部署标识（6位随机字符串）
  deployedTime: string | null;   // 部署时间
  userId: number;                // 创建者 ID
  
  // 计算属性（前端生成）
  isDeployed: boolean;           // 是否已部署
  deployUrl: string | null;      // 部署 URL
  previewUrl: string;            // 预览 URL
}
```

### 6.2 部署状态判断

```javascript
// 判断是否已部署
function isDeployed(app) {
  return app.deployKey != null && app.deployedTime != null;
}

// 获取部署 URL
function getDeployUrl(app) {
  if (!isDeployed(app)) return null;
  const deployDomain = import.meta.env.VITE_DEPLOY_DOMAIN;
  return `${deployDomain}/${app.deployKey}/`;
}

// 获取预览 URL
function getPreviewUrl(app) {
  const previewDomain = import.meta.env.VITE_PREVIEW_DOMAIN;
  return `${previewDomain}/${app.codeGenType}_${app.id}/`;
}
```

---

## 七、目录结构

### 7.1 预览目录（生成代码存储）

```
tmp/code_output/
├── HTML_123/              # HTML 应用预览
│   └── index.html
├── HTML_456/
│   └── index.html
└── MULTI_FILE_789/        # 多文件应用预览
    ├── index.html
    ├── styles/
    │   └── main.css
    └── scripts/
        └── app.js
```

### 7.2 部署目录（已部署应用存储）

```
tmp/code_deploy/
├── aB3xYz/                # deployKey = aB3xYz
│   └── index.html
├── cD4eFg/
│   └── index.html
└── hI5jKl/
    ├── index.html
    ├── styles/
    │   └── main.css
    └── scripts/
        └── app.js
```

---

## 八、注意事项

### 8.1 部署限制

- 必须先生成代码才能部署
- 仅应用所有者可以部署
- 部署后的应用公开可访问（无需登录）

### 8.2 deployKey 管理

- deployKey 是 6 位随机字符串（大小写字母 + 数字）
- 每个应用只有一个 deployKey
- 重复部署会更新部署内容，deployKey 不变
- 删除应用会级联删除部署目录

### 8.3 URL 构建

**前端环境变量**：
```javascript
// 部署域名
VITE_DEPLOY_DOMAIN=http://localhost:8123/api/static  // 开发环境
VITE_DEPLOY_DOMAIN=https://apps.yoursite.com        // 生产环境

// 预览域名
VITE_PREVIEW_DOMAIN=http://localhost:8123/api/preview
VITE_PREVIEW_DOMAIN=https://preview.yoursite.com
```

**后端配置**（application.yml）：
```yaml
app:
  deploy:
    host: http://localhost:8123/api/static  # 部署域名
  preview:
    host: http://localhost:8123/api/preview # 预览域名
```

### 8.4 性能考虑

- 部署操作会复制整个目录
- 大量文件可能需要几秒钟
- 建议在前端显示加载状态

### 8.5 安全考虑

- 已部署应用公开可访问
- 不要在生成的代码中包含敏感信息
- deployKey 无法预测（随机生成）

---

## 九、常见问题

### Q1: 部署后如何更新应用？

**A**: 重新生成代码后，再次点击"部署"按钮即可更新部署内容。deployKey 保持不变，访问地址不变。

### Q2: 如何分享已部署的应用？

**A**: 部署成功后，复制返回的 URL 分享给他人即可访问。

### Q3: 部署的应用会过期吗？

**A**: 不会过期，除非删除应用。

### Q4: 预览和部署有什么区别？

**A**:
- **预览**：临时的，每次生成代码会覆盖，适合开发测试
- **部署**：稳定的，生成固定 URL，适合分享和长期访问

### Q5: 部署失败怎么办？

**A**: 常见原因：
1. 未生成代码 → 先生成代码
2. 代码生成失败 → 检查 AI 生成日志
3. 网络问题 → 稍后重试

---

## 十、错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40000 | 应用 ID 无效 |
| 40100 | 用户未登录 |
| 40101 | 无权限部署该应用 |
| 40400 | 应用不存在 |
| 50000 | 应用代码不存在，请先生成代码 |
| 50001 | 部署失败 |

---

## 十一、完整示例

### 11.1 部署应用完整流程

```javascript
// 1. 用户点击"部署"按钮
async function handleDeploy(appId) {
  // 显示加载状态
  showLoading('正在部署...');
  
  try {
    // 2. 调用部署接口
    const response = await fetch('/api/app/deploy', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({ appId })
    });
    
    const result = await response.json();
    
    // 3. 处理响应
    if (result.code === 0) {
      // 4. 显示成功提示
      showDeploySuccess(result.data);
    } else {
      // 5. 显示错误信息
      showError(result.message);
    }
  } catch (error) {
    showError('部署失败，请稍后重试');
  } finally {
    hideLoading();
  }
}

// 6. 显示部署成功对话框
function showDeploySuccess(deployUrl) {
  const dialog = {
    title: '部署成功！',
    message: `访问地址：${deployUrl}`,
    buttons: [
      {
        text: '复制链接',
        onClick: () => {
          navigator.clipboard.writeText(deployUrl);
          showToast('链接已复制到剪贴板');
        }
      },
      {
        text: '立即访问',
        onClick: () => window.open(deployUrl, '_blank')
      },
      {
        text: '关闭'
      }
    ]
  };
  
  showDialog(dialog);
}
```

---

**文档版本**: v1.0  
**创建时间**: 2026-03-08  
**后端开发**: zkf