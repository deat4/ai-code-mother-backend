# AI代码生成JSON解析问题分析报告

## 问题概述

在HTML/MULTI_FILE类型的代码生成中，保存的文件内容是原始的JSON字符串格式（如 `{ "html": "\n\n..." }`），而不是解析后的实际HTML内容。

**问题表现**：
用户看到生成的 `index.html` 文件内容类似：
```
{ "html": "\n\n\n \n \n \n \n\n\n \n
\n
\n
ModernShop
...
```

这表明JSON字符串被直接保存为文件内容，而非解析后的HTML代码。

## 根本原因（已确认）

**LangChain4j的OpenAI兼容层在反序列化SSE响应时，已将JSON中的 `\n` 转义序列转换为实际换行符。**

这意味着当 `Flux<String>` 中的所有token拼接完成后，得到的字符串中：
- JSON字符串值内的 `\n` 转义序列已经变成实际的换行符(0x0A)
- 导致拼接后的内容违反JSON规范（字符串值内不能有未转义换行符）
- JSON解析器会抛出 "Unterminated string" 错误
- **原来的fallback逻辑直接返回原始内容，导致用户看到JSON外壳**

## 已实施的多策略修复方案

### HtmlCodeParser 修复策略

```
拼接完成的原始内容
    │
    ├─ 策略1: JSON转义修复 → JSON解析 → 提取html字段 ✅
    │   (escapeNewlinesInJsonStringValues + JSONUtil.parseObj)
    │
    ├─ 策略2: 正则提取（绕过JSON解析） ✅
    │   (直接找到 "html": " 后的内容，提取到结束引号)
    │
    ├─ 策略3: 暴力剥离JSON外壳 ✅
    │   (去掉 {"html": " 前缀和 "} 后缀，反转义)
    │
    ├─ 策略4: 激进提取HTML标签 ✅
    │   (找到第一个 <!DOCTYPE 或 <html 标签位置)
    │
    └─ 最终fallback: 移除JSON外壳后返回
```

### MultiFileCodeParser 修复策略

类似的多策略架构，针对 `{"html": "...", "css": "...", "js": "..."}` 结构。

### 关键修复点

1. **换行符转义修复** (`escapeNewlinesInJsonStringValues`)：
   - 遍历JSON字符串，追踪是否在字符串值内部
   - 在字符串值内遇到实际换行符时，转换为 `\n` 转义序列

2. **正则提取** (`tryRegexExtract`)：
   - 不依赖JSON解析器
   - 直接从 `"html": "` 后提取内容
   - 从末尾向前找未转义的结束引号

3. **激进剥离** (`tryStripJsonWrapper`)：
   - 直接去除 `{"html": "` 前缀
   - 直接去除 `"}` 后缀
   - 反转义残留的转义序列

4. **fallback逻辑改进**：
   - 不再直接返回原始JSON字符串
   - 而是尝试移除JSON外壳
   - 或激进提取HTML标签内容

## 代码生成流程

```
用户请求 → AiCodeGeneratorFacade.generateAndSaveCodeStream()
→ AiCodeGeneratorService.generateHtmlCodeStream() [返回 Flux<String>]
→ processCodeStream() [拼接所有流式token]
→ CodeParserExecutor.executeParser() [解析完整内容]
→ HtmlCodeParser.parseCode() [多策略提取HTML内容]
→ CodeFileSaverExecutor.executeSaver() [保存文件]
```

## 相关文件

| 文件 | 作用 |
|------|------|
| `HtmlCodeParser.java` | HTML JSON解析器（已修复） |
| `MultiFileCodeParser.java` | 多文件 JSON解析器（已修复） |
| `AiCodeGeneratorFacade.java` | 流式处理入口 |
| `StreamingChatModelConfig.java` | 流式模型配置 |

## 环境信息

- Java 21
- Spring Boot 3.5.11
- LangChain4j 1.1.0
- DeepSeek API (通过 OpenAI兼容接口)
- Hutool JSON解析器

---

**文档创建时间**：2026-04-22  
**问题状态**：已修复，待验证