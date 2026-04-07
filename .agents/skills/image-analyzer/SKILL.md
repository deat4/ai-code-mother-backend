---
name: image-analyzer
description: 当用户上传图片、截图或需要分析视觉内容时，自动调用此 skill。即使当前模型（如 GLM-5）不支持图片输入，也可以通过 MCP 工具调用 Qwen3.5 Plus 进行图片分析。
metadata:
  author: User
  version: "1.0.0"
  trigger: image_upload, screenshot, visual_analysis, ocr, ui_review
  mcp_tool: qwen-vision
---

# Image Analyzer - 自动图片分析

## 自动触发条件

当检测到以下情况时，**自动调用 `analyze_image` MCP 工具**：

1. 用户上传图片文件
2. 用户粘贴截图
3. 用户询问图片内容
4. 需要 OCR 文字识别
5. 需要分析 UI/界面设计
6. 需要理解图表、流程图、地图

## 工具调用流程

```
用户上传图片
    ↓
检测当前模型是否支持图片 (GLM-5 不支持)
    ↓
自动调用 qwen-vision MCP 工具
    ↓
使用 Qwen3.5 Plus 分析图片
    ↓
将分析结果返回给用户
```

## MCP 工具

### `analyze_image` (来自 qwen-vision MCP)

使用 Qwen3.5 Plus 分析图片。

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `image_data` | string | ✅ | Base64 编码的图片数据（包含 `data:image/xxx;base64,` 前缀） |
| `prompt` | string | ❌ | 分析提示，默认 "请详细描述这张图片的内容" |
| `analysis_type` | string | ❌ | 分析类型：`describe`, `ocr`, `ui`, `chart`, `map`, `code` |

**分析类型说明：**

| 类型 | 用途 | 自动生成的提示 |
|------|------|---------------|
| `describe` | 通用描述 | 详细描述图片内容、元素、颜色、布局 |
| `ocr` | 文字识别 | 识别图片中所有文字，按原格式输出 |
| `ui` | UI 分析 | 分析界面设计、布局、组件、交互元素 |
| `chart` | 图表分析 | 分析图表类型、数据趋势、关键信息 |
| `map` | 地图分析 | 分析地理位置、地形、地标 |
| `code` | 代码识别 | 识别图片中的代码或代码结构图 |

### `check_image_capability`

检查当前模型是否支持图片输入。

## 使用示例

### 示例 1：用户上传截图

```
用户: [上传截图]
AI: 我来分析这张截图... [调用 skill_mcp mcp_name="qwen-vision" tool_name="analyze_image"]
    [Qwen3.5 Plus 分析结果] 这是一个 React 组件的代码截图...
```

### 示例 2：OCR 文字识别

```
用户: 帮我识别这张图片里的文字
AI: [调用 analyze_image，analysis_type="ocr"]
    [识别结果] 图片中的文字内容如下：...
```

## 技术配置

- **MCP 服务器**: `qwen-vision`
- **后端模型**: Qwen3.5 Plus
- **API 端点**: `https://coding.dashscope.aliyuncs.com/v1`
- **超时时间**: 60 秒

## 注意事项

1. **API 限制**：图片大小不超过 API 限制
2. **支持格式**：JPEG, PNG, WebP, GIF
3. **自动降级**：如果 Qwen API 不可用，会返回错误信息