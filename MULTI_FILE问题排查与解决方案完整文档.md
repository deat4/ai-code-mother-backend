# MULTI_FILE 模式问题排查与解决方案完整文档

## 文档概述

本文档记录了 MULTI_FILE 模式代码生成过程中遇到的所有问题、排查过程和最终解决方案。

**文档生成时间**：2026-04-23
**问题状态**：已完全修复

---

## 问题一：Markdown 标记未去除

### 问题描述

MULTI_FILE 模式生成的代码文件，开头包含 ````html` 标记，结尾包含 ```` 标记，导致浏览器无法正确解析 HTML，预览页面显示为纯文本而非渲染后的页面。

**问题文件示例**：
```
文件开头：```html
<!DOCTYPE html>
...

文件结尾：
</body>
</html>
```

### 根因分析

经过多轮排查，发现以下问题：

#### 根因1：`substring(12)` 应该是 `substring(13)`

`stripCodeMarkers()` 和 `stripMarkdownCodeBlock()` 方法中，对 `"```javascript"` 使用了 `substring(12)`，但该字符串长度为 13，会导致 JS 文件开头残留字符 't'。

**修复位置**：`MultiFileCodeParser.java:339, 357`

**修复代码**：
```java
// 修复前
int offset = trimmed.startsWith("```javascript") ? 12 : 5;

// 修复后
int offset = trimmed.startsWith("```javascript") ? 13 : 5;
```

#### 根因2：`findClosingQuote()` 从前往后搜索导致误判

原方法从前往后搜索结束引号，遇到 HTML 属性中的引号（如 `class="container"`）会截断位置判断错误。

**修复方案**：改为"按下一个字段 key 切分"的方式。

```java
// 新增常量
private static final Pattern NEXT_FIELD_PATTERN = Pattern.compile(
    "\\s*,\\s*\"(?:html|css|js|htmlCode|cssCode|jsCode)\"\\s*:"
);

// 新方法
private int findFieldEnd(String text, int startPos) {
    Matcher nextFieldMatcher = NEXT_FIELD_PATTERN.matcher(text);
    if (nextFieldMatcher.find(startPos)) {
        int end = nextFieldMatcher.start();
        // 去掉字段值结尾的空白和引号...
        return end;
    }
    // 找 JSON 对象的结束位置...
}
```

#### 根因3：保存层缺少兜底清洗

即使解析器修复了，AI 输出始终是不稳定输入，需要最后一层保险。

**修复位置**：`CodeFileSaverTemplate.java`

**修复代码**：
```java
private String sanitizeBeforeWrite(String filename, String content) {
    // 去除 markdown fence 标记
    // 根据文件类型处理...
    if (trimmed.startsWith("```")) {
        int firstLineBreak = trimmed.indexOf('\n');
        if (firstLineBreak > 0) {
            trimmed = trimmed.substring(firstLineBreak + 1).trim();
        }
    }
    if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
    }
    return trimmed;
}
```

---

## 问题二：CSS/JS 内容丢失

### 问题描述

MULTI_FILE 模式生成的页面，CSS 文件存在但内容为空或截断，JS 文件不存在，预览页面没有样式和交互功能。

### 根因分析

经过深入排查，发现这是多层问题叠加：

#### 根因1：枚举值定义不一致（最高优先级）

两个 `CodeGenTypeEnum` 类的 value 定义不一致：

| 枚举类 | HTML | MULTI_FILE | getEnumByValue 方法 |
|--------|------|------------|---------------------|
| `model.enums.CodeGenTypeEnum` | "HTML" | **"multi_file" (小写)** | 精确匹配 |
| `core.CodeGenTypeEnum` | "HTML" | **"MULTI_FILE" (大写)** | 忽略大小写 |

当数据库存储 `"MULTI_FILE"`（大写）时，`model.enums.CodeGenTypeEnum.getEnumByValue("MULTI_FILE")` 返回 null，系统警告并回退到 HTML 模式。

**日志证据**：
```
应用 404558651423965184 的代码生成类型无效: MULTI_FILE，使用默认值 HTML
为 appId: ..., codeGenType: HTML 创建新的 AI 服务实例
```

**修复方案**：
1. 统一 `model.enums.CodeGenTypeEnum.MULTI_FILE` value 为 `"MULTI_FILE"`（大写）
2. `getEnumByValue()` 改为 `equalsIgnoreCase()` 忽略大小写匹配

```java
// model/enums/CodeGenTypeEnum.java
MULTI_FILE("原生多文件模式", "MULTI_FILE"),  // 改为大写

public static CodeGenTypeEnum getEnumByValue(String value) {
    for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
        if (anEnum.value.equalsIgnoreCase(value)) {  // 改为 equalsIgnoreCase
            return anEnum;
        }
    }
    return null;
}
```

#### 根因2：`looksLikeJsonWrapped()` 只认短字段名

该方法只检测 `"html"`、`"css"`、`"js"`，没有把 `"htmlCode"`、`"cssCode"`、`"jsCode"` 算进去。AI 返回长字段名 JSON 时，解析器连"这是 JSON"都认不出来。

**修复代码**：
```java
private boolean looksLikeJsonWrapped(String content) {
    String trimmed = content.trim();
    return trimmed.startsWith("{") && (
            trimmed.contains("\"html\"") ||
            trimmed.contains("\"css\"") ||
            trimmed.contains("\"js\"") ||
            trimmed.contains("\"htmlCode\"") ||
            trimmed.contains("\"cssCode\"") ||
            trimmed.contains("\"jsCode\"")
    );
}
```

#### 根因3：正则模式只匹配短字段名

策略2正则提取时，`HTML_KEY_PATTERN` 等只匹配 `"html"` 等短字段名。

**修复方案**：支持两种字段名匹配

```java
private static final Pattern HTML_KEY_PATTERN = 
    Pattern.compile("\"(?:html|htmlCode)\"\\s*:\\s*\"");
private static final Pattern CSS_KEY_PATTERN = 
    Pattern.compile("\"(?:css|cssCode)\"\\s*:\\s*\"");
private static final Pattern JS_KEY_PATTERN = 
    Pattern.compile("\"(?:js|jsCode)\"\\s*:\\s*\"");
```

#### 根因4：资源路径不匹配（最终根因）

AI 生成的 HTML 引用子目录资源（`css/style.css`、`js/app.js`），但部署器只保存根目录文件（`style.css`、`script.js`），导致浏览器请求 404。

**证据**：
```
浏览器请求：
/api/static/4aBt5U/css/style.css  → 404
/api/static/4aBt5U/js/app.js      → 404

实际部署文件：
index.html
style.css   (根目录，非 css/)
script.js   (根目录，非 js/)
```

**修复方案**：两层修复

1. **Prompt 约束**：强制 AI 只输出三个根目录文件

```text
## 输出格式要求（必须严格遵守）
1. 只允许输出 3 个文件：
   - index.html
   - style.css
   - script.js

2. 禁止创建子目录
3. HTML 只能引用 href="style.css" 和 src="script.js"
```

2. **路径修正**：保存前修正 HTML 资源路径

```java
private String normalizeHtmlAssetPaths(String html) {
    // css/style.css -> style.css
    html = html.replaceAll("href\\s*=\\s*\"(?:\\./)?css/[^\"/]+\\.css\"", "href=\"style.css\"");
    // js/app.js -> script.js
    html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?js/[^\"/]+\\.js\"", "src=\"script.js\"");
    return html;
}
```

---

## 问题三：API 请求超时

### 问题描述

DeepSeek API 流式响应超过默认超时时间（60秒），导致 `Read timed out` 错误。

### 解决方案

在 `application-location.yml` 中添加超时配置：

```yaml
langchain4j:
  open-ai:
    chat-model:
      timeout: 180s
    streaming-chat-model:
      timeout: 180s
    reasoning-streaming-chat-model:
      timeout: 300s
    routing-chat-model:
      timeout: 60s
```

---

## 修复层级总结

| 层级 | 修复位置 | 作用 |
|------|----------|------|
| **枚举层** | `CodeGenTypeEnum` value 统一 | 正确识别 MULTI_FILE 类型 |
| **检测层** | `looksLikeJsonWrapped()` | 识别 JSON 格式（支持长短字段名） |
| **内嵌层** | `extractEmbeddedJsonBlock()` | 提取内嵌 JSON |
| **正则层** | `HTML_KEY_PATTERN` 等 | 支持匹配长短字段名 |
| **切分层** | `findFieldEnd()` | 按字段 key 切分，避免引号干扰 |
| **清洗层** | `stripCodeMarkers()` | 去除 markdown 标记 |
| **路径层** | `normalizeHtmlAssetPaths()` | 修正 HTML 资源路径 |
| **保存层** | `sanitizeBeforeWrite()` | 兜底清洗 |
| **Prompt层** | 约束输出格式 | 约束 AI 输出结构 |
| **配置层** | timeout 参数 | 防止 API 超时 |

---

## 测试验证

修复后重新生成 MULTI_FILE 应用，预期结果：

1. ✅ 日志不再显示"代码生成类型无效"警告
2. ✅ 日志正确显示 `codeGenType: MULTI_FILE`
3. ✅ 生成的文件无 markdown 标记残留
4. ✅ HTML 引用 `href="style.css"` 和 `src="script.js"`
5. ✅ 浏览器成功加载 CSS 和 JS（无 404）
6. ✅ 页面样式和交互正常

---

## 相关文件

- `问题排查文档-MULTI_FILE-Markdown标记未去除.md` - Markdown 标记问题详细排查
- `问题排查文档-MULTI_FILE-CSS-JS内容丢失.md` - CSS/JS 丢失问题详细排查
- `src/main/java/com/zkf/aicodemother/core/parser/MultiFileCodeParser.java` - 解析器修复
- `src/main/java/com/zkf/aicodemother/core/saver/CodeFileSaverTemplate.java` - 保存层修复
- `src/main/java/com/zkf/aicodemother/model/enums/CodeGenTypeEnum.java` - 枚举修复
- `src/main/resources/prompt/codegen-multi-file-system-prompt.txt` - Prompt 约束

---

## 结论

本次问题排查的核心教训：

1. **枚举一致性至关重要**：项目中存在两个同名枚举类时，务必确保 value 定义和匹配方法一致
2. **多层防护更稳健**：解析层、保存层都需要清洗，不能只靠单层处理
3. **路径匹配要考虑实际部署结构**：AI 输出结构与部署器结构必须一致，否则需要修正
4. **Prompt 约束是源头治理**：通过约束 AI 输出格式，减少后端处理负担

---

**文档维护者**：开发团队
**最后更新**：2026-04-23