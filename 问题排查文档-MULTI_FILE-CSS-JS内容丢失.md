# MULTI_FILE 模式 CSS/JS 内容丢失问题排查报告

## 问题描述

**问题现象**：MULTI_FILE 模式生成的页面，CSS 文件存在但内容为空或截断，JS 文件不存在。

**问题文件示例**：
```
目录：tmp/code_output/MULTI_FILE_404554473209298944/

index.html：正常（约100行完整内容）
style.css：仅包含截断内容（2行）
script.js：文件不存在

对比正常生成：
目录：tmp/code_output/MULTI_FILE_391877036155543552/
index.html、styles.css、script.js：均为完整内容
```

## 问题根因分析

### 根因1：字段名不匹配（主要问题）

**问题详情**：

| 定义位置 | 字段名 |
|----------|--------|
| `MultiFileCodeResult.java` (AI 模型输出类) | `htmlCode`, `cssCode`, `jsCode` |
| `MultiFileCodeParser.java` (正则模式) | `html`, `css`, `js` |

AI 模型根据 `@Description` 注解返回 JSON 格式，字段名与类定义一致。但解析器的正则模式查找的是短字段名。

**代码对比**：

```java
// MultiFileCodeResult.java - AI 返回的字段名
@Description("HTML代码")
private String htmlCode;

@Description("CSS代码")
private String cssCode;

@Description("JS代码")
private String jsCode;
```

```java
// MultiFileCodeParser.java - 解析器查找的字段名（错误）
private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"html\"\\s*:\\s*\"");
private static final Pattern CSS_KEY_PATTERN = Pattern.compile("\"css\"\\s*:\\s*\"");
private static final Pattern JS_KEY_PATTERN = Pattern.compile("\"js\"\\s*:\\s*\"");
```

**影响分析**：

当 AI 返回 JSON 格式 `{htmlCode: "...", cssCode: "...", jsCode: "..."}` 时：

| 解析策略 | 处理方式 | 结果 |
|----------|----------|------|
| 策略1: JSON 解析 (`extractFromJsonObject`) | 同时查找两种字段名 | ✅ 正常 |
| 策略2: 正则提取 (`tryRegexExtractMultiFile`) | 只查找短字段名 | ❌ 失败 |

当策略1因换行符问题失败时，回退到策略2，导致 CSS 和 JS 内容无法提取。

### 根因2：分隔符格式正则模式问题

**问题详情**：

分隔符格式的正则模式使用了 reluctant quantifier 配合 lookahead，在边界情况下可能匹配到空字符串。

```java
// extractFromDelimiterFormat 方法中的正则
Pattern filePattern = Pattern.compile("---文件名:\\s*(.+?)---\\s*\\n([\\s\\S]*?)(?=---文件名:|$)");
```

IDE 警告：`Fix this reluctant quantifier that will only ever match 0 repetitions.`

**问题分析**：
- `([\\s\\S]*?)` 使用 reluctant 匹配，尽可能少匹配
- 配合 lookahead `(?=---文件名:|$)` 查找下一个分隔符或结尾
- 在某些边界情况下（如文件内容为空或只有一个文件），可能匹配失败

### 根因3：Prompt 与 AI 输出格式不一致

**Prompt 定义**：

```text
## 输出格式
使用以下格式输出多个文件：

---文件名: filename1---
文件内容

---文件名: filename2---
文件内容
```

**实际 AI 输出**：

AI 模型可能返回两种格式：
1. 分隔符格式：`---文件名: xxx---`（符合 prompt）
2. JSON 格式：`{htmlCode: "...", cssCode: "...", jsCode: "..."}`（LangChain4j 自动结构化）

当 AI 返回 JSON 格式时，与 prompt 定义的分隔符格式不符，解析流程可能偏离预期。

### 根因4：文件路径不匹配

**问题详情**：

AI 生成的 HTML 中引用的路径与实际保存的文件路径不一致。

```html
<!-- AI 生成的引用 -->
<link rel="stylesheet" href="css/style.css">
<link rel="stylesheet" href="css/responsive.css">
```

```java
// 实际保存的文件名（MultiFileCodeFileSaverTemplate.java）
writeToFile(baseDirPath, "index.html", result.getHtmlCode());
writeToFile(baseDirPath, "style.css", result.getCssCode());  // 根目录，不是 css 子目录
writeToFile(baseDirPath, "script.js", result.getJsCode());   // 根目录，不是子目录
```

**影响**：即使 CSS 内容正确保存，浏览器也无法正确加载，因为路径不匹配。

## 代码处理流程分析

### 1. 整体流程

```
用户请求 → AI 返回流式响应 → Flux<String> 收集完整内容
→ CodeParserExecutor.executeParser() → MultiFileCodeParser.parseCode()
→ 解析策略判断：
  ├─ 策略1: JSON 转义修复 + JSON 解析 → extractFromJsonObject() [兼容两种字段名]
  ├─ 策略2: 正则提取 → tryRegexExtractMultiFile() [只查找短字段名] ❌
  ├─ 分隔符格式: extractFromDelimiterFormat() [正则模式问题]
  ├─ 代码块提取: extractCodeByPattern()
  └─ 激进提取: aggressiveExtract()
→ CodeFileSaverExecutor.executeSaver() → MultiFileCodeFileSaverTemplate.saveFiles()
→ writeToFile() 写入固定文件名
```

### 2. 关键代码文件

#### 2.1 MultiFileCodeResult.java (AI 模型输出类)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/ai/model/MultiFileCodeResult.java

@Description("生成多个代码文件的结果")
@Data
public class MultiFileCodeResult {
    @Description("HTML代码")
    private String htmlCode;  // 字段名：htmlCode

    @Description("CSS代码")
    private String cssCode;   // 字段名：cssCode

    @Description("JS代码")
    private String jsCode;    // 字段名：jsCode

    @Description("生成代码的描述")
    private String description;
}
```

#### 2.2 MultiFileCodeParser.java (解析器)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/parser/MultiFileCodeParser.java

// 问题：正则模式字段名不匹配
private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"html\"\\s*:\\s*\"");
private static final Pattern CSS_KEY_PATTERN = Pattern.compile("\"css\"\\s*:\\s*\"");
private static final Pattern JS_KEY_PATTERN = Pattern.compile("\"js\"\\s*:\\s*\"");

// 正则提取方法（策略2）
private MultiFileCodeResult tryRegexExtractMultiFile(String content) {
    MultiFileCodeResult result = new MultiFileCodeResult();
    String trimmed = content.trim();

    // 问题：查找的是 "html" 而不是 "htmlCode"
    String html = tryExtractSingleField(trimmed, HTML_KEY_PATTERN);
    if (html != null) {
        result.setHtmlCode(stripCodeMarkers(html));
    }

    // 问题：查找的是 "css" 而不是 "cssCode"
    String css = tryExtractSingleField(trimmed, CSS_KEY_PATTERN);
    if (css != null) {
        result.setCssCode(stripCodeMarkers(css));
    }

    // 问题：查找的是 "js" 而不是 "jsCode"
    String js = tryExtractSingleField(trimmed, JS_KEY_PATTERN);
    if (js != null) {
        result.setJsCode(stripCodeMarkers(js));
    }

    return result;
}

// JSON 解析方法（策略1）- 正确处理两种字段名
private MultiFileCodeResult extractFromJsonObject(JSONObject json) {
    MultiFileCodeResult result = new MultiFileCodeResult();

    String html = json.getStr("html");        // 短字段名
    String css = json.getStr("css");
    String js = json.getStr("js");
    String htmlCode = json.getStr("htmlCode"); // 长字段名
    String cssCode = json.getStr("cssCode");
    String jsCode = json.getStr("jsCode");

    if (html != null) result.setHtmlCode(stripCodeMarkers(html.trim()));
    if (htmlCode != null && result.getHtmlCode() == null) result.setHtmlCode(stripCodeMarkers(htmlCode.trim()));
    if (css != null) result.setCssCode(stripCodeMarkers(css.trim()));
    if (cssCode != null && result.getCssCode() == null) result.setCssCode(stripCodeMarkers(cssCode.trim()));
    if (js != null) result.setJsCode(stripCodeMarkers(js.trim()));
    if (jsCode != null && result.getJsCode() == null) result.setJsCode(stripCodeMarkers(jsCode.trim()));

    return result;
}

// 分隔符格式提取 - 正则模式问题
private MultiFileCodeResult extractFromDelimiterFormat(String content) {
    MultiFileCodeResult result = new MultiFileCodeResult();

    // 问题：reluctant quantifier 可能匹配失败
    Pattern filePattern = Pattern.compile("---文件名:\\s*(.+?)---\\s*\\n([\\s\\S]*?)(?=---文件名:|$)");
    Matcher matcher = filePattern.matcher(content);

    while (matcher.find()) {
        String fileName = matcher.group(1).trim();
        String fileContent = matcher.group(2).trim();

        // 问题：文件名匹配范围有限
        if (fileName.endsWith(".html") || fileName.equalsIgnoreCase("index.html")) {
            result.setHtmlCode(stripCodeMarkers(fileContent));
        } else if (fileName.endsWith(".css") || fileName.equalsIgnoreCase("style.css")) {
            result.setCssCode(stripCodeMarkers(fileContent));  // 只匹配 style.css
        } else if (fileName.endsWith(".js") || fileName.equalsIgnoreCase("app.js") || fileName.equalsIgnoreCase("main.js")) {
            result.setJsCode(stripCodeMarkers(fileContent));
        }
        // 问题：未匹配 styles.css, script.js 等其他常见文件名
    }

    return result;
}
```

#### 2.3 MultiFileCodeFileSaverTemplate.java (保存器)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/saver/MultiFileCodeFileSaverTemplate.java

@Override
protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
    // 问题：固定文件名，不支持子目录
    writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    writeToFile(baseDirPath, "style.css", result.getCssCode());  // 固定 style.css
    writeToFile(baseDirPath, "script.js", result.getJsCode());   // 固定 script.js
}
```

## 需要排查的关键点

### 1. AI 原始返回内容格式

需要确认 AI 返回的 JSON 字段名：
- `{"htmlCode": "...", "cssCode": "...", "jsCode": "..."}`（长字段名）
- 还是 `{"html": "...", "css": "...", "js": "..."}`（短字段名）

### 2. 解析器日志输出

检查日志中显示的成功策略：
- "策略1成功: JSON转义修复后解析多文件"
- "策略2成功: 正则提取多文件字段"
- 或其他策略

### 3. CSS/JS 内容提取验证

在 `tryRegexExtractMultiFile` 方法中添加日志，确认：
- CSS 和 JS 字段是否被成功匹配
- 匹配到的内容长度

### 4. 文件名匹配范围

检查 `extractFromDelimiterFormat` 中文件名匹配是否覆盖所有常见文件名：
- `styles.css` vs `style.css`
- `script.js` vs `app.js` vs `main.js`

## 建议修复方案

### 修复1：统一字段名匹配（优先级高）

修改正则模式，同时支持两种字段名：

```java
// 修改 HTML_KEY_PATTERN 等正则模式
private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"(?:html|htmlCode)\"\\s*:\\s*\"");
private static final Pattern CSS_KEY_PATTERN = Pattern.compile("\"(?:css|cssCode)\"\\s*:\\s*\"");
private static final Pattern JS_KEY_PATTERN = Pattern.compile("\"(?:js|jsCode)\"\\s*:\\s*\"");
```

同时修改 `tryExtractSingleField` 方法处理两种匹配结果。

### 修复2：改进分隔符格式正则

修改正则模式，避免 reluctant quantifier 问题：

```java
// 使用 greedy quantifier 配合明确的结束标记
Pattern filePattern = Pattern.compile("---文件名:\\s*([^\\n]+)---\\s*\\n([\\s\\S]+?)(?=---文件名:---|$)");
```

### 修复3：扩展文件名匹配范围

在 `extractFromDelimiterFormat` 中扩展文件名匹配：

```java
// 扩展 CSS 文件名匹配
if (fileName.endsWith(".css") || fileName.matches("(style|styles|main|app)\\.css")) {
    result.setCssCode(stripCodeMarkers(fileContent));
}

// 扩展 JS 文件名匹配
if (fileName.endsWith(".js") || fileName.matches("(script|app|main|index)\\.js")) {
    result.setJsCode(stripCodeMarkers(fileContent));
}
```

### 修复4：支持动态文件名保存

修改保存逻辑，支持从解析结果中获取实际文件名：

```java
// MultiFileCodeResult 增加文件名字段
private String htmlFileName;
private String cssFileName;
private String jsFileName;

// 保存器使用动态文件名
writeToFile(baseDirPath, result.getHtmlFileName() != null ? result.getHtmlFileName() : "index.html", result.getHtmlCode());
```

## 联系信息

如有问题，请联系开发人员。

---
文档生成时间：2026-04-22
问题状态：已完全修复

## 最终根因：资源路径不匹配

**问题描述**：AI 生成的 HTML 引用子目录资源（`css/style.css`、`js/app.js`），但部署器只保存根目录文件（`style.css`、`script.js`），导致浏览器请求 404。

**证据**：
```
浏览器请求：
/api/static/4aBt5U/css/style.css  → 404
/api/static/4aBt5U/js/app.js      → 404

实际部署文件：
.../code_deploy/4aBt5U/index.html
.../code_deploy/4aBt5U/style.css   (根目录，非 css/)
.../code_deploy/4aBt5U/script.js   (根目录，非 js/)
```

## 已应用的修复方案（完整版）

### 修复0：枚举值定义不一致（最高优先级）

详见上文。

### 修复1-4：解析器改进（详见上文）

- `looksLikeJsonWrapped()` 支持长短字段名
- 正则模式支持长短字段名
- 分隔符格式改进
- 调试日志添加

### 修复5：Prompt 约束输出格式（新增）

**问题根因**：原 prompt 没有约束文件数量和路径结构，AI 自然生成多目录多文件结构。

**修复内容**：更新 `codegen-multi-file-system-prompt.txt`，强制约束：
- 只允许输出 3 个文件：`index.html`、`style.css`、`script.js`
- 禁止创建子目录
- 禁止引用不存在的文件
- HTML 只能引用 `href="style.css"` 和 `src="script.js"`

### 修复6：保存前修正 HTML 资源路径（新增）

**问题根因**：即使 prompt 约束了，AI 可能仍生成子目录引用。

**修复位置**：`CodeFileSaverTemplate.sanitizeBeforeWrite()` 方法

**修复代码**：
```java
private String normalizeHtmlAssetPaths(String html) {
    // 修正 CSS 引用：css/style.css, css/responsive.css 等 -> style.css
    html = html.replaceAll("href\\s*=\\s*\"(?:\\./)?css/[^\"/]+\\.css\"", "href=\"style.css\"");

    // 修正 JS 引用：js/app.js, js/data.js 等 -> script.js
    html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?js/[^\"/]+\\.js\"", "src=\"script.js\"");

    // 修正常见变体
    html = html.replaceAll("href\\s*=\\s*\"(?:\\./)?styles\\.css\"", "href=\"style.css\"");
    html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?app\\.js\"", "src=\"script.js\"");
    html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?main\\.js\"", "src=\"script.js\"");

    return html;
}
```

## 修复层级总结（最终版）

| 层级 | 方法 | 作用 |
|------|------|------|
| 枚举层 | `CodeGenTypeEnum` value 统一 | 正确识别 MULTI_FILE 类型 |
| 检测层 | `looksLikeJsonWrapped()` | 识别 JSON 格式 |
| 内嵌层 | `extractEmbeddedJsonBlock()` | 提取内嵌 JSON |
| 解析层 | 各策略 + `stripCodeMarkers()` | 提取并清洗内容 |
| 切分层 | `findFieldEnd()` | 按字段 key 切分 |
| **路径层** | `normalizeHtmlAssetPaths()` | 修正 HTML 资源路径 |
| 保存层 | `sanitizeBeforeWrite()` | 兜底清洗 |
| **Prompt层** | 约束输出格式 | 约束 AI 输出结构 |

## 测试建议

重新生成 MULTI_FILE 应用，观察：
1. 日志显示正确类型识别（无警告）
2. 生成的 `index.html` 引用 `href="style.css"` 和 `src="script.js"`
3. 浏览器成功加载 CSS 和 JS（无 404）
4. 页面样式和交互正常

## 已应用的修复方案（完整版）

### 修复0：枚举值定义不一致（最高优先级，最前置根因）

**问题根因**：两个 `CodeGenTypeEnum` 类的 value 定义不一致，导致系统无法识别 `MULTI_FILE` 类型。

| 枚举类 | HTML | MULTI_FILE | VUE_PROJECT | getEnumByValue 方法 |
|--------|------|------------|-------------|---------------------|
| `model.enums.CodeGenTypeEnum` | "HTML" | **"multi_file" (小写)** | "vue_project" | 精确匹配 `equals()` |
| `core.CodeGenTypeEnum` | "HTML" | **"MULTI_FILE" (大写)** | "vue_project" | 忽略大小写 `equalsIgnoreCase()` |

当数据库存储 `"MULTI_FILE"`（大写）时：
- `model.enums.CodeGenTypeEnum.getEnumByValue("MULTI_FILE")` 返回 **null**
- 系统警告：`代码生成类型无效: MULTI_FILE，使用默认值 HTML`
- 后续流程使用 **HTML 模式** 的 AI 服务实例

**日志证据**：
```
应用 404558651423965184 的代码生成类型无效: MULTI_FILE，使用默认值 HTML
为 appId: 404558651423965184, codeGenType: HTML 创建新的 AI 服务实例
```

**修复方案**：

1. 统一 `model.enums.CodeGenTypeEnum` 的 value 定义：
```java
MULTI_FILE("原生多文件模式", "MULTI_FILE"),  // 改为大写，与 core 版本一致
```

2. 使用忽略大小写匹配：
```java
public static CodeGenTypeEnum getEnumByValue(String value) {
    for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
        if (anEnum.value.equalsIgnoreCase(value)) {  // 改为 equalsIgnoreCase
            return anEnum;
        }
    }
    return null;
}
```

### 修复1：`looksLikeJsonWrapped()` 同时支持长短字段名（最前置的根因）

**问题根因**：该方法只检测 `"html"`、`"css"`、`"js"` 短字段名，导致 AI 返回 `"htmlCode"/"cssCode"/"jsCode"` 长字段名的 JSON 时，解析器连"这是 JSON"都认不出来，不会进入策略1和策略2，直接掉到 fallback。

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

### 修复2：正则模式同时支持长短字段名

**问题根因**：`HTML_KEY_PATTERN` 等正则只匹配 `"html"` 等短字段名，策略2无法提取 `"htmlCode"` 等长字段名的内容。

**修复代码**：

```java
// 匹配各字段的键（同时支持短字段名和长字段名）
private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"(?:html|htmlCode)\"\\s*:\\s*\"");
private static final Pattern CSS_KEY_PATTERN = Pattern.compile("\"(?:css|cssCode)\"\\s*:\\s*\"");
private static final Pattern JS_KEY_PATTERN = Pattern.compile("\"(?:js|jsCode)\"\\s*:\\s*\"");
```

### 修复3：分隔符格式提取改进

**问题根因**：
1. 正则模式使用 reluctant quantifier 可能匹配失败
2. 文件名匹配范围太窄（只特判几个固定文件名）

**修复代码**：

```java
private MultiFileCodeResult extractFromDelimiterFormat(String content) {
    MultiFileCodeResult result = new MultiFileCodeResult();

    // 改进正则模式，避免 reluctant quantifier 问题
    Pattern filePattern = Pattern.compile("---文件名:\\s*([^\\n]+)---\\s*\\n([\\s\\S]*?)(?=---文件名:\\s*[^\\n]+---|$)");
    Matcher matcher = filePattern.matcher(content);

    while (matcher.find()) {
        String fileName = matcher.group(1).trim();
        String fileContent = matcher.group(2).trim();

        // 简化文件名判断：只根据扩展名分类（覆盖所有 css/js 文件）
        if (fileName.endsWith(".html")) {
            result.setHtmlCode(stripCodeMarkers(fileContent));
        } else if (fileName.endsWith(".css")) {
            result.setCssCode(stripCodeMarkers(fileContent));
        } else if (fileName.endsWith(".js")) {
            result.setJsCode(stripCodeMarkers(fileContent));
        }
    }

    return result;
}
```

### 修复4：添加关键调试日志

在解析流程中添加日志，快速定位问题：

```java
// 关键调试日志：追踪解析流程
log.info("原始内容前200字符: {}", content.substring(0, Math.min(200, content.length())));
log.info("looksLikeJsonWrapped = {}", looksLikeJsonWrapped(content));

// 记录解析结果（包含所有三个字段）
log.info("解析结果: htmlCode长度={}, cssCode长度={}, jsCode长度={}",
        result.getHtmlCode() != null ? result.getHtmlCode().length() : null,
        result.getCssCode() != null ? result.getCssCode().length() : null,
        result.getJsCode() != null ? result.getJsCode().length() : null);
```

## 修复优先级说明

按问题影响程度排序：

| 优先级 | 问题 | 影响 |
|--------|------|------|
| **最高** | `looksLikeJsonWrapped()` 只认短字段名 | JSON 格式无法识别，直接掉到 fallback |
| **高** | 正则模式只匹配短字段名 | 策略2 无法提取长字段名内容 |
| **中** | 分隔符正则/文件名匹配问题 | 分隔符格式提取不稳定 |
| **低** | HTML 引用路径与保存路径不一致 | 文件存在但浏览器无法加载 |

## 待后续处理的问题

**保存路径问题**：当前保存器固定使用 `index.html`、`style.css`、`script.js`，但 AI 生成的 HTML 可能引用 `css/style.css`、`js/script.js` 等子目录路径。

短期解决方案建议：
- 方案 A：约束 AI 输出（prompt 要求引用根目录文件）
- 方案 B：保存前修正 HTML 引用路径

## 测试建议

重新生成 MULTI_FILE 应用，观察：
1. 日志显示 `looksLikeJsonWrapped = true/false`
2. 日志显示三个字段长度（确认 CSS/JS 是否被提取）
3. 生成的文件内容是否完整
4. 浏览器预览样式是否正常