package com.zkf.aicodemother.core.parser;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * 采用多策略解析：
 * 1. JSON转义修复后标准解析
 * 2. 正则提取各字段值
 * 3. 代码块提取
 * 4. 分隔符格式提取
 *
 * @author yupi
 */
@Slf4j
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    // 匹配 markdown 代码块
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile(
            "^```(?:json|\\w*)\\s*\\n?([\\s\\S]*?)\\n?\\s*```$",
            Pattern.DOTALL
    );

    // 匹配 ```json 代码块
    private static final Pattern JSON_CODE_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    // 匹配各类型代码块
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    // 匹配各字段的键（同时支持短字段名和长字段名）
    private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"(?:html|htmlCode)\"\\s*:\\s*\"");
    private static final Pattern CSS_KEY_PATTERN = Pattern.compile("\"(?:css|cssCode)\"\\s*:\\s*\"");
    private static final Pattern JS_KEY_PATTERN = Pattern.compile("\"(?:js|jsCode)\"\\s*:\\s*\"");

    // 匹配下一个字段键（用于按字段边界切分，比依赖引号状态判断更稳健）
    private static final Pattern NEXT_FIELD_PATTERN = Pattern.compile(
            "\\s*,\\s*\"(?:html|css|js|htmlCode|cssCode|jsCode)\"\\s*:"
    );

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            return new MultiFileCodeResult();
        }

        MultiFileCodeResult result = new MultiFileCodeResult();
        String content = codeContent.trim();

        // Step 0: 去除最外层 markdown 代码块包裹
        content = stripMarkdownCodeBlock(content);

        // Step 0.5: 提取内嵌的 JSON 代码块（处理 AI 在 JSON 前后添加说明文字的情况）
        content = extractEmbeddedJsonBlock(content);

        // 关键调试日志：追踪解析流程
        log.info("原始内容前200字符: {}", content.substring(0, Math.min(200, content.length())));
        log.info("looksLikeJsonWrapped = {}", looksLikeJsonWrapped(content));

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

            log.warn("所有JSON解析策略均失败，尝试其他方法");
        }

        // Step 2: 从分隔符格式提取（prompt要求的格式）
        result = extractFromDelimiterFormat(content);
        if (hasValidContent(result)) {
            return result;
        }

        // Step 3: 从代码块提取（fallback）
        String htmlCode = extractCodeByPattern(content, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(content, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(content, JS_CODE_PATTERN);

        if (htmlCode != null) result.setHtmlCode(htmlCode.trim());
        if (cssCode != null) result.setCssCode(cssCode.trim());
        if (jsCode != null) result.setJsCode(jsCode.trim());

        // Step 4: 如果仍然没有有效内容，尝试激进提取
        if (!hasValidContent(result)) {
            result = aggressiveExtract(content);
        }

        // 记录解析结果用于调试（包含所有三个字段）
        log.info("解析结果: htmlCode长度={}, cssCode长度={}, jsCode长度={}",
                result.getHtmlCode() != null ? result.getHtmlCode().length() : null,
                result.getCssCode() != null ? result.getCssCode().length() : null,
                result.getJsCode() != null ? result.getJsCode().length() : null);

        return result;
    }

    // ========== 检测方法 ==========

    /**
     * 检测内容是否看起来像 JSON 格式
     */
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

    // ========== 策略1: JSON转义修复 ==========

    /**
     * 提取内嵌的 JSON 代码块
     * 处理 AI 在 JSON 前后添加说明文字的情况，如：
     * "下面是生成结果：
     * ```json
     * {...}
     * ```"
     */
    private String extractEmbeddedJsonBlock(String content) {
        Matcher matcher = JSON_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content;
    }

    private MultiFileCodeResult tryParseWithNewlineEscape(String content) {
        try {
            String fixed = escapeNewlinesInJsonStringValues(content.trim());
            JSONObject obj = JSONUtil.parseObj(fixed);
            return extractFromJsonObject(obj);
        } catch (Exception e) {
            log.debug("策略1失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 在JSON字符串值内部，将实际换行符转义为 \\n
     */
    private String escapeNewlinesInJsonStringValues(String json) {
        StringBuilder result = new StringBuilder(json.length() + 1024);
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            if (inString) {
                if (c == '\n') {
                    result.append("\\n");
                } else if (c == '\r') {
                    result.append("\\r");
                } else if (c == '\t') {
                    result.append("\\t");
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // ========== 策略2: 正则提取 ==========

    /**
     * 用正则提取多文件JSON的各字段值
     */
    private MultiFileCodeResult tryRegexExtractMultiFile(String content) {
        try {
            MultiFileCodeResult result = new MultiFileCodeResult();
            String trimmed = content.trim();

            // 提取 html 字段
            String html = tryExtractSingleField(trimmed, HTML_KEY_PATTERN);
            if (html != null) {
                result.setHtmlCode(stripCodeMarkers(html));
            }

            // 提取 css 字段
            String css = tryExtractSingleField(trimmed, CSS_KEY_PATTERN);
            if (css != null) {
                result.setCssCode(stripCodeMarkers(css));
            }

            // 提取 js 字段
            String js = tryExtractSingleField(trimmed, JS_KEY_PATTERN);
            if (js != null) {
                result.setJsCode(stripCodeMarkers(js));
            }

            return result;
        } catch (Exception e) {
            log.debug("策略2失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取单个字段的值
     */
    private String tryExtractSingleField(String content, Pattern keyPattern) {
        Matcher keyMatcher = keyPattern.matcher(content);
        if (!keyMatcher.find()) {
            return null;
        }

        int valueStart = keyMatcher.end();
        int valueEnd = findFieldEnd(content, valueStart);

        if (valueEnd < 0) {
            return null;
        }

        String rawValue = content.substring(valueStart, valueEnd);
        return unescapeJsonString(rawValue);
    }

    /**
     * 按下一个字段 key 切分，找到当前字段值的结束位置
     * 这种方式比依赖引号状态判断更稳健，能处理 HTML 属性中的未转义引号
     */
    private int findFieldEnd(String text, int startPos) {
        Matcher nextFieldMatcher = NEXT_FIELD_PATTERN.matcher(text);
        if (nextFieldMatcher.find(startPos)) {
            int end = nextFieldMatcher.start();

            // 去掉字段值结尾的空白和引号
            while (end > startPos && Character.isWhitespace(text.charAt(end - 1))) {
                end--;
            }
            if (end > startPos && text.charAt(end - 1) == '"') {
                end--;
            }
            return end;
        }

        // 如果没有下一个字段，找 JSON 对象的结束位置
        int end = text.lastIndexOf('}');
        if (end < 0) {
            end = text.length();
        }

        // 去掉 } 前的空白和引号
        while (end > startPos && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end > startPos && text.charAt(end - 1) == '}') {
            end--;
        }
        while (end > startPos && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end > startPos && text.charAt(end - 1) == '"') {
            end--;
        }

        return end;
    }

    /**
     * 反转义JSON字符串中的转义序列
     */
    private String unescapeJsonString(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ========== 工具方法 ==========

    /**
     * 去除 Markdown 代码块包裹
     */
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
        if (trimmed.startsWith("```css")) {
            return trimmed.substring(6).trim();
        }
        if (trimmed.startsWith("```js") || trimmed.startsWith("```javascript")) {
            int offset = trimmed.startsWith("```javascript") ? 13 : 5;
            return trimmed.substring(offset).trim();
        }
        if (trimmed.startsWith("```")) {
            return trimmed.substring(3).trim();
        }

        // 处理结尾的 ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        return trimmed;
    }

    /**
     * 从 JSONObject 中提取多文件代码结果
     */
    private MultiFileCodeResult extractFromJsonObject(JSONObject json) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 提取各字段
        String html = json.getStr("html");
        String css = json.getStr("css");
        String js = json.getStr("js");
        String htmlCode = json.getStr("htmlCode");
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

    /**
     * 从分隔符格式提取（---文件名: xxx---）
     */
    private MultiFileCodeResult extractFromDelimiterFormat(String content) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 匹配 ---文件名: xxx--- 格式（改进正则模式）
        Pattern filePattern = Pattern.compile("---文件名:\\s*([^\\n]+)---\\s*\\n([\\s\\S]*?)(?=---文件名:\\s*[^\\n]+---|$)");
        Matcher matcher = filePattern.matcher(content);

        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            String fileContent = matcher.group(2).trim();

            // 简化文件名判断：只根据扩展名分类
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

    /**
     * 检查是否有有效内容
     */
    private boolean hasValidContent(MultiFileCodeResult result) {
        return result.getHtmlCode() != null || result.getCssCode() != null || result.getJsCode() != null;
    }

    /**
     * 根据正则模式提取代码，并去除可能的markdown标记
     */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            // 去除可能残留的markdown标记
            return stripCodeMarkers(extracted);
        }
        return null;
    }

    /**
     * 去除代码内容中可能残留的markdown标记
     */
    private String stripCodeMarkers(String code) {
        String trimmed = code.trim();
        // 去除开头的 ```html, ```css, ```js 等
        if (trimmed.startsWith("```html")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```css")) {
            trimmed = trimmed.substring(6).trim();
        } else if (trimmed.startsWith("```js")) {
            trimmed = trimmed.substring(5).trim();
        } else if (trimmed.startsWith("```javascript")) {
            trimmed = trimmed.substring(13).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }
        // 去除结尾的 ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    /**
     * 激进提取：当所有策略都失败时，尝试从内容中提取看起来像代码的部分
     */
    private MultiFileCodeResult aggressiveExtract(String content) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 尝试找HTML标签
        String html = extractHtmlTags(content);
        if (html != null) {
            result.setHtmlCode(html);
        }

        // 尝试找CSS规则
        String css = extractCssRules(content);
        if (css != null) {
            result.setCssCode(css);
        }

        // 尝试找JS代码
        String js = extractJsCode(content);
        if (js != null) {
            result.setJsCode(js);
        }

        return result;
    }

    /**
     * 提取HTML标签内容
     */
    private String extractHtmlTags(String content) {
        // 先去除可能的markdown标记
        content = stripCodeMarkers(content);

        int htmlStart = content.indexOf("<!DOCTYPE");
        if (htmlStart < 0) htmlStart = content.indexOf("<html");
        if (htmlStart < 0) htmlStart = content.indexOf("<head");
        if (htmlStart < 0) htmlStart = content.indexOf("<body");
        if (htmlStart < 0) htmlStart = content.indexOf("<div");

        if (htmlStart >= 0) {
            String extracted = content.substring(htmlStart);
            int lastTagEnd = extracted.lastIndexOf('>');
            if (lastTagEnd > 0) {
                extracted = extracted.substring(0, lastTagEnd + 1);
            }
            return extracted.trim();
        }
        return null;
    }

    /**
     * 提取CSS规则内容
     */
    private String extractCssRules(String content) {
        // 先去除可能的markdown标记
        content = stripCodeMarkers(content);

        // 查找 { } 包裹的CSS规则
        int cssStart = content.indexOf("body {");
        if (cssStart < 0) cssStart = content.indexOf("* {");
        if (cssStart < 0) cssStart = content.indexOf(".");
        if (cssStart < 0) cssStart = content.indexOf("#");

        if (cssStart >= 0) {
            // 找到CSS块的结束位置
            int braceCount = 0;
            int end = -1;
            for (int i = cssStart; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        end = i;
                        break;
                    }
                }
            }
            if (end > cssStart) {
                return content.substring(cssStart, end + 1).trim();
            }
        }
        return null;
    }

    /**
     * 提取JS代码内容
     */
    private String extractJsCode(String content) {
        // 先去除可能的markdown标记
        content = stripCodeMarkers(content);

        int jsStart = content.indexOf("function");
        if (jsStart < 0) jsStart = content.indexOf("const ");
        if (jsStart < 0) jsStart = content.indexOf("let ");
        if (jsStart < 0) jsStart = content.indexOf("var ");
        if (jsStart < 0) jsStart = content.indexOf("document.");

        if (jsStart >= 0) {
            String extracted = content.substring(jsStart);
            // 找JS代码结束位置（简单处理）
            int lastBrace = extracted.lastIndexOf('}');
            if (lastBrace > 0) {
                extracted = extracted.substring(0, lastBrace + 1);
            }
            return extracted.trim();
        }
        return null;
    }
}