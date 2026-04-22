# MULTI_FILE 代码生成 Markdown 标记未去除问题排查文档

## 问题描述

**问题现象**：MULTI_FILE 模式生成的代码文件，开头包含 ````html` 标记，结尾包含 ```` 标记，导致浏览器无法正确解析 HTML，预览页面显示为纯文本而非渲染后的页面。

**问题文件示例**：
```
文件路径：E:\BankProject\ai-code-mother\ai-code-mother\tmp\code_output\MULTI_FILE_404548138925219840\index.html

文件开头内容：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    ...

文件结尾内容：
</body>
</html>
```

## 问题影响

1. 预览页面显示纯文本而非渲染后的页面
2. 部署后的页面同样显示纯文本
3. 用户无法正常查看生成的应用

## 代码处理流程分析

### 1. 整体流程

```
用户请求 → AI 返回流式响应 → Flux<String> 收集完整内容
→ CodeParserExecutor.executeParser() → MultiFileCodeParser.parseCode()
→ CodeFileSaverExecutor.executeSaver() → MultiFileCodeFileSaverTemplate.saveFiles()
→ writeToFile() 写入文件
```

### 2. 关键代码文件

#### 2.1 AiCodeGeneratorFacade.java (流式处理)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/AiCodeGeneratorFacade.java
// 关键方法: processCodeStream()

private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId, String userMessage, Long userId) {
    StringBuilder codeBuilder = new StringBuilder();
    return codeStream
            .doOnNext(codeBuilder::append)  // 收集所有流式 chunk
            .doOnComplete(() -> {
                try {
                    String completeCode = codeBuilder.toString();  // 获取完整内容
                    // 使用执行器解析代码
                    Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                    // 使用执行器保存代码
                    File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                } catch (Exception e) {
                    log.error("代码保存失败: {}", e.getMessage(), e);
                }
            });
}
```

**问题点**：`codeBuilder.toString()` 获取的是 AI 原始返回内容，可能包含 markdown 标记。

#### 2.2 CodeParserExecutor.java (解析执行器)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/parser/CodeParserExecutor.java

public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
    return switch (codeGenType) {
        case HTML -> htmlCodeParser.parseCode(codeContent);
        case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
        case VUE_PROJECT -> throw new BusinessException(...);
    };
}
```

#### 2.3 MultiFileCodeParser.java (解析器 - 最新版本)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/parser/MultiFileCodeParser.java

@Override
public MultiFileCodeResult parseCode(String codeContent) {
    if (codeContent == null || codeContent.trim().isEmpty()) {
        return new MultiFileCodeResult();
    }

    MultiFileCodeResult result = new MultiFileCodeResult();
    String content = codeContent.trim();

    // Step 0: 去除 markdown 代码块包裹
    content = stripMarkdownCodeBlock(content);

    // Step 1: 检测是否是 JSON 格式
    if (looksLikeJsonWrapped(content)) {
        // 策略1: 修复换行符后做标准JSON解析
        result = tryParseWithNewlineEscape(content);
        if (result != null && hasValidContent(result)) {
            log.info("策略1成功: JSON转义修复后解析多文件");
            return result;
        }

        // 策略2: 正则提取各字段
        result = tryRegexExtractMultiFile(content);
        if (result != null && hasValidContent(result)) {
            log.info("策略2成功: 正则提取多文件字段");
            return result;
        }
    }

    // Step 2: 从分隔符格式提取
    // Step 3: 从代码块提取
    // Step 4: 激进提取
    ...
}

// 去除 Markdown 代码块包裹
private String stripMarkdownCodeBlock(String content) {
    String trimmed = content.trim();

    Matcher matcher = MARKDOWN_CODE_BLOCK_PATTERN.matcher(trimmed);
    if (matcher.matches()) {
        return matcher.group(1).trim();
    }

    // 处理没有结束标记的情况
    if (trimmed.startsWith("```json")) {
        return trimmed.substring(7).trim();
    }
    if (trimmed.startsWith("```html")) {
        return trimmed.substring(7).trim();
    }
    ...
    if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
    }

    return trimmed;
}

// 正则模式定义
private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile(
        "^```(?:json|\\w*)\\s*\\n?([\\s\\S]*?)\\n?\\s*```$",
        Pattern.DOTALL
);
```

#### 2.4 MultiFileCodeFileSaverTemplate.java (保存器)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/saver/MultiFileCodeFileSaverTemplate.java

@Override
protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
    // 保存 HTML 文件
    writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    // 保存 CSS 文件
    writeToFile(baseDirPath, "style.css", result.getCssCode());
    // 保存 JavaScript 文件
    writeToFile(baseDirPath, "script.js", result.getJsCode());
}
```

#### 2.5 CodeFileSaverTemplate.java (保存模板基类)

```java
// 文件路径: src/main/java/com/zkf/aicodemother/core/saver/CodeFileSaverTemplate.java

protected void writeToFile(String baseDirPath, String fileName, String content) {
    if (StrUtil.isBlank(content)) {
        return;
    }
    String filePath = baseDirPath + "/" + fileName;
    try {
        FileUtil.writeString(content, filePath, "UTF-8");
        log.info("文件保存成功: {}", filePath);
    } catch (Exception e) {
        log.error("文件保存失败: {}", filePath, e);
    }
}
```

## 问题根因分析

### 假设1: stripMarkdownCodeBlock 方法未生效

**分析**：
- `MARKDOWN_CODE_BLOCK_PATTERN` 正则：`^```(?:json|\w*)\s*\n?([\s\S]*?)\n?\s*```$`
- 该正则要求内容完全匹配（从头到尾），但 AI 返回的内容可能是：
  - JSON 格式：`{"html": "...", "css": "...", "js": "..."}`
  - 内部嵌套 markdown：JSON 字符串值内部包含 ````html` 和 ````

**验证**：查看 AI 返回的原始内容格式。

### 假设2: JSON 解析策略失败，回退到其他策略

**分析**：
- 当 JSON 解析失败时，可能回退到分隔符格式提取或激进提取
- 这些回退策略可能没有正确处理 markdown 标记

### 假设3: MultiFileCodeResult 中的 htmlCode 字段未经过 stripCodeMarkers 处理

**需要验证**：
- 检查各策略提取结果是否正确调用了 stripCodeMarkers 方法
- 检查 result.getHtmlCode() 返回的内容是否包含 markdown 标记

## 需要排查的关键点

### 1. AI 原始返回内容格式

需要获取 AI 流式响应完成后的 `codeBuilder.toString()` 原始内容，确认：
- 是否是 JSON 格式 `{"html": "...", "css": "...", "js": "..."}`
- JSON 字符串值内部是否包含 ````html` 和 ```` 标记
- 整体内容是否被 ````json ... ```` 包裹

### 2. 解析器日志输出

检查日志中显示的成功策略：
- "策略1成功: JSON转义修复后解析多文件" 还是
- "策略2成功: 正则提取多文件字段" 还是
- 其他策略

### 3. MultiFileCodeParser 各策略的 stripCodeMarkers 调用情况

检查以下方法是否正确调用了 stripCodeMarkers：
- `extractFromJsonObject()` - JSON 解析结果处理
- `tryRegexExtractMultiFile()` - 正则提取结果处理
- `extractFromDelimiterFormat()` - 分隔符格式提取处理

### 4. CSS 文件内容检查

检查 style.css 文件是否也有类似问题（开头/结尾有 markdown 标记）。

## 附：相关文件完整内容

### MultiFileCodeParser.java 完整代码

见文件：`src/main/java/com/zkf/aicodemother/core/parser/MultiFileCodeParser.java`

### MultiFileCodeFileSaverTemplate.java 完整代码

见文件：`src/main/java/com/zkf/aicodemother/core/saver/MultiFileCodeFileSaverTemplate.java`

### CodeFileSaverTemplate.java 完整代码

见文件：`src/main/java/com/zkf/aicodemother/core/saver/CodeFileSaverTemplate.java`

## 建议排查步骤

1. **添加调试日志**：在 `AiCodeGeneratorFacade.processCodeStream()` 的 `doOnComplete()` 中添加：
   ```java
   log.info("AI原始返回内容前200字符: {}", completeCode.substring(0, Math.min(200, completeCode.length())));
   log.info("AI原始返回内容后200字符: {}", completeCode.substring(Math.max(0, completeCode.length() - 200)));
   ```

2. **添加解析结果日志**：在 `MultiFileCodeParser.parseCode()` 返回前添加：
   ```java
   log.info("解析结果 htmlCode 前100字符: {}", result.getHtmlCode() != null ? result.getHtmlCode().substring(0, Math.min(100, result.getHtmlCode().length())) : "null");
   ```

3. **添加保存前日志**：在 `writeToFile()` 方法中添加：
   ```java
   log.info("写入文件内容前100字符: {}", content.substring(0, Math.min(100, content.length())));
   ```

4. **重新测试**：生成新的 MULTI_FILE 应用，观察日志输出。

## 联系信息

如有问题，请联系开发人员。

---
文档生成时间：2026-04-22
问题状态：已完全修复（第二轮增强）

## 已应用的修复方案（完整版）

### 修复1: ```javascript 截断长度错误修正

**问题根因**：`substring(12)` 应该是 `substring(13)`，因为 `"```javascript"` 的长度是 13，会导致 JS 文件开头残留字符 't'。

**修复位置**：`MultiFileCodeParser.java`
- 第339行：`int offset = trimmed.startsWith("```javascript") ? 13 : 5;`
- 第357行：`trimmed = trimmed.substring(13).trim();`

### 修复2: 提取内嵌 JSON 代码块

**问题根因**：定义了 `JSON_CODE_PATTERN` 但未使用，当 AI 在 JSON 前后添加说明文字时，解析流程不会进入 JSON 解析分支。

**修复方案**：添加 `extractEmbeddedJsonBlock` 方法并在 `parseCode` 开头调用。

```java
// 新增方法
private String extractEmbeddedJsonBlock(String content) {
    Matcher matcher = JSON_CODE_PATTERN.matcher(content);
    if (matcher.find()) {
        return matcher.group(1).trim();
    }
    return content;
}

// parseCode 中调用
String content = codeContent.trim();
content = stripMarkdownCodeBlock(content);
content = extractEmbeddedJsonBlock(content);  // 新增
```

### 修复3: 按下一个字段 key 切分

**问题根因**：`findClosingQuote` 和 `isInString` 方法依赖引号状态判断，当 AI 返回的 JSON 中 HTML 属性引号未转义时，会误判字段边界。

**修复方案**：改用"按下一个字段 key 切分"的方式，新增 `NEXT_FIELD_PATTERN` 和 `findFieldEnd` 方法。

```java
// 新增常量
private static final Pattern NEXT_FIELD_PATTERN = Pattern.compile(
    "\\s*,\\s*\"(?:html|css|js|htmlCode|cssCode|jsCode)\"\\s*:"
);

// 新增方法
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

### 修复4: 保存层兜底清洗

**问题根因**：即使解析器修复了，AI 输出始终是不稳定输入，需要最后一层保险。

**修复方案**：在 `CodeFileSaverTemplate.writeToFile` 方法中添加 `sanitizeBeforeWrite` 清洗逻辑。

```java
// CodeFileSaverTemplate.java 新增方法
private String sanitizeBeforeWrite(String filename, String content) {
    String trimmed = content.trim();

    // 根据文件类型去除对应的语言标记
    if (filename.endsWith(".html")) {
        trimmed = stripFence(trimmed, "html");
    } else if (filename.endsWith(".css")) {
        trimmed = stripFence(trimmed, "css");
    } else if (filename.endsWith(".js")) {
        trimmed = stripFence(trimmed, "javascript");
        trimmed = stripFence(trimmed, "js");
    }

    // 兜底去除无语言标记的 ```
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

## 修复层级总结

| 层级 | 方法 | 作用 |
|------|------|------|
| 最外层 | `stripMarkdownCodeBlock()` | 去除整段内容的 markdown 包裹 |
| 内嵌层 | `extractEmbeddedJsonBlock()` | 从说明文字中提取纯 JSON |
| 解析层 | `stripCodeMarkers()` | 各策略提取后去除残留标记 |
| 切分层 | `findFieldEnd()` | 按字段 key 切分，不依赖引号状态 |
| 保存层 | `sanitizeBeforeWrite()` | 兜底清洗，最后一道防线 |

## 测试建议

重新生成 MULTI_FILE 应用，观察：
1. 日志显示的成功策略（策略1还是策略2）
2. 新生成的 `index.html` 开头是否还有 ```html 标记
3. 浏览器预览是否恢复正常渲染
4. JS 文件开头是否残留 't' 字符