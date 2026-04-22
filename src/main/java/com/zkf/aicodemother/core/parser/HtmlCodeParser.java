package com.zkf.aicodemother.core.parser;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 * 支持多种格式：```json代码块、纯JSON、```html代码块、纯HTML
 *
 * 采用多策略解析：
 * 1. JSON转义修复后标准解析
 * 2. 正则直接提取 "html" 字段的值（绕过JSON解析器）
 * 3. 暴力剥离JSON外壳
 *
 * @author yupi
 */
@Slf4j
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    // 匹配 markdown 代码块
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile(
            "^```(?:json|html|\\w*)\\s*\\n?([\\s\\S]*?)\\n?\\s*```$",
            Pattern.DOTALL
    );

    // 匹配 "html" 键后面的冒号和开始引号
    private static final Pattern HTML_KEY_PATTERN = Pattern.compile("\"html\"\\s*:\\s*\"");

    // 匹配 JSON 外壳开始 {"html": "
    private static final Pattern JSON_WRAPPER_START_PATTERN = Pattern.compile("\\{\\s*\"html\"\\s*:\\s*\"");

    // 匹配 ```html 代码块
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            return new HtmlCodeResult();
        }

        HtmlCodeResult result = new HtmlCodeResult();
        String content = codeContent.trim();

        // Step 0: 去除 markdown 代码块包裹
        content = stripMarkdownCodeBlock(content);

        // Step 1: 检测是否是 JSON 包装格式
        if (looksLikeJsonWrapped(content)) {
            // 策略1: 修复换行符后做标准JSON解析
            String htmlCode = tryParseWithNewlineEscape(content);
            if (htmlCode != null && !htmlCode.trim().isEmpty()) {
                log.info("策略1成功: JSON转义修复后解析");
                result.setHtmlCode(htmlCode.trim());
                return result;
            }

            // 策略2: 用正则直接提取 "html" 字段的值（不依赖JSON解析器）
            htmlCode = tryRegexExtract(content);
            if (htmlCode != null && !htmlCode.trim().isEmpty()) {
                log.info("策略2成功: 正则提取html字段值");
                result.setHtmlCode(htmlCode.trim());
                return result;
            }

            // 策略3: 智能剥离JSON外壳
            htmlCode = tryStripJsonWrapper(content);
            if (htmlCode != null && !htmlCode.trim().isEmpty()) {
                log.info("策略3成功: 剥离JSON外壳");
                result.setHtmlCode(htmlCode.trim());
                return result;
            }

            log.warn("所有JSON解析策略均失败，尝试其他方法");
        }

        // Step 2: 尝试从 ```html 代码块提取
        String htmlCode = extractHtmlCode(content);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
            return result;
        }

        // Step 3: 最终fallback - 返回清理后的内容
        // 注意：如果内容以 { 开头，说明是JSON格式但解析失败，需要特殊处理
        String cleanedContent = cleanContent(content);
        if (!cleanedContent.trim().isEmpty()) {
            // 验证清理后的内容是否有效（不应以JSON外壳开头）
            String trimmed = cleanedContent.trim();
            if (!trimmed.startsWith("{") || !trimmed.contains("\"html\"")) {
                result.setHtmlCode(trimmed);
                return result;
            }
            // 如果清理后仍然是JSON外壳格式，尝试更激进的提取
            log.warn("清理后的内容仍为JSON外壳格式，尝试激进提取");
            htmlCode = aggressiveExtract(trimmed);
            if (htmlCode != null) {
                result.setHtmlCode(htmlCode);
                return result;
            }
        }

        // 最坏情况：返回原始内容但去除明显的JSON外壳
        result.setHtmlCode(removeJsonWrapperIfPresent(content));
        return result;
    }

    // ========== 检测方法 ==========

    /**
     * 检测内容是否看起来像 JSON 包装的 HTML
     */
    private boolean looksLikeJsonWrapped(String content) {
        String trimmed = content.trim();
        // 以 { 开头，并且包含 "html" 键
        return trimmed.startsWith("{") && trimmed.contains("\"html\"");
    }

    // ========== 策略1: JSON转义修复 ==========

    private String tryParseWithNewlineEscape(String content) {
        try {
            String fixed = escapeNewlinesInJsonStringValues(content.trim());
            JSONObject obj = JSONUtil.parseObj(fixed);
            String html = extractHtmlFromJsonObject(obj);
            if (html != null && !html.isBlank()) {
                return html;
            }
        } catch (Exception e) {
            log.debug("策略1失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 在JSON字符串值内部，将实际换行符转义为 \\n
     * 核心思路：追踪是否在字符串值内部，如果是，则转义换行符
     */
    private String escapeNewlinesInJsonStringValues(String json) {
        StringBuilder result = new StringBuilder(json.length() + 1024);
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                // 前一个字符是反斜杠，当前字符是转义序列的一部分
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                // 在字符串内遇到反斜杠，标记转义
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
                // 在字符串值内部
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
     * 用正则找到 "html" : " 之后的内容，一直到最后一个 " }
     * 这个方法不依赖JSON解析器，直接做文本提取
     */
    private String tryRegexExtract(String content) {
        try {
            String trimmed = content.trim();

            Matcher keyMatcher = HTML_KEY_PATTERN.matcher(trimmed);
            if (!keyMatcher.find()) {
                return null;
            }

            int valueStart = keyMatcher.end(); // "html": " 之后的位置

            // 从末尾向前找最后一个未转义的引号（JSON对象的结束位置前）
            int valueEnd = findClosingQuote(trimmed, valueStart);

            if (valueEnd < 0) {
                return null;
            }

            String rawValue = trimmed.substring(valueStart, valueEnd);

            // rawValue 现在是JSON字符串值的内容（未经JSON反转义）
            // 由于换行符已经是实际换行符了，直接使用即可
            // 但还需要处理其他JSON转义序列（如 \" → "，\\ → \ 等）
            String result = unescapeJsonString(rawValue);

            // 去除可能的markdown标记
            return stripCodeMarkers(result);

        } catch (Exception e) {
            log.debug("策略2失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从指定位置开始，找到字符串值的结束引号
     * 需要跳过转义的引号 \"
     */
    private int findClosingQuote(String text, int startPos) {
        // 从末尾向前搜索，找到 "} 模式（允许中间有空白）
        // 这比从前往后找更可靠，因为HTML内容中可能有复杂的引号
        int lastBrace = text.lastIndexOf('}');
        if (lastBrace < 0) return -1;

        // 从 } 往前找最近的未转义的 "
        for (int i = lastBrace - 1; i >= startPos; i--) {
            if (text.charAt(i) == '"') {
                // 检查这个引号是否被转义
                int backslashCount = 0;
                for (int j = i - 1; j >= 0 && text.charAt(j) == '\\'; j--) {
                    backslashCount++;
                }
                if (backslashCount % 2 == 0) {
                    // 未转义的引号，这就是结束位置
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 反转义JSON字符串中的转义序列
     * 注意：由于LangChain4j已经将 \n 转为实际换行符，
     * 这里主要处理 \" 和 \\ 等还残留的转义
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

    // ========== 策略3: 暴力剥离JSON外壳 ==========

    /**
     * 直接用文本操作去掉 {"html": " 前缀和 "} 后缀
     */
    private String tryStripJsonWrapper(String content) {
        try {
            String trimmed = content.trim();

            Matcher m = JSON_WRAPPER_START_PATTERN.matcher(trimmed);
            if (!m.find() || m.start() != 0) {
                return null;
            }

            String inner = trimmed.substring(m.end());

            // 去掉末尾的 "}（可能有空白）
            inner = inner.replaceAll("\"\\s*\\}\\s*$", "");

            // 反转义
            String result = unescapeJsonString(inner);

            // 去除可能的markdown标记
            result = stripCodeMarkers(result);

            // 基本验证：结果应该包含HTML标签
            if (result.contains("<") && result.contains(">")) {
                return result;
            }
        } catch (Exception e) {
            log.debug("策略3失败: {}", e.getMessage());
        }
        return null;
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
        if (trimmed.startsWith("```")) {
            return trimmed.substring(3).trim();
        }

        return content;
    }

    /**
     * 从 JSON 对象中提取 HTML
     */
    private String extractHtmlFromJsonObject(JSONObject json) {
        // 1. 尝试 "html" 字段（prompt 要求的格式）
        String html = json.getStr("html");
        if (html != null && !html.trim().isEmpty()) {
            return stripCodeMarkers(html);
        }

        // 2. 尝试 "htmlCode" 字段（LangChain4j 序列化格式）
        String htmlCode = json.getStr("htmlCode");
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            return stripCodeMarkers(htmlCode);
        }

        return null;
    }

    /**
     * 提取 HTML 代码块内容
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
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
        // 去除开头的 ```html, ```json 等
        if (trimmed.startsWith("```html")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7).trim();
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
     * 清理内容（移除 markdown 标记等）
     */
    private String cleanContent(String content) {
        String cleaned = content;

        // 移除结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        // 移除开头的 ```html, ```json 标记
        if (cleaned.startsWith("```html")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        return cleaned.trim();
    }

    /**
     * 激进提取：当所有策略都失败时，尝试直接提取看起来像HTML的内容
     */
    private String aggressiveExtract(String content) {
        // 先去除markdown标记
        content = stripCodeMarkers(content);

        // 尝试找到第一个HTML标签的位置
        int htmlStart = content.indexOf("<!DOCTYPE");
        if (htmlStart < 0) {
            htmlStart = content.indexOf("<html");
        }
        if (htmlStart < 0) {
            htmlStart = content.indexOf("<head");
        }
        if (htmlStart < 0) {
            htmlStart = content.indexOf("<body");
        }
        if (htmlStart < 0) {
            htmlStart = content.indexOf("<div");
        }
        if (htmlStart < 0) {
            htmlStart = content.indexOf("<");
        }

        if (htmlStart >= 0) {
            String extracted = content.substring(htmlStart);
            // 从末尾找最后一个HTML标签结束位置
            int lastTagEnd = extracted.lastIndexOf('>');
            if (lastTagEnd > 0) {
                extracted = extracted.substring(0, lastTagEnd + 1);
            }
            return extracted.trim();
        }

        return null;
    }

    /**
     * 移除JSON外壳（如果存在）
     */
    private String removeJsonWrapperIfPresent(String content) {
        String trimmed = content.trim();

        // 如果以 { 开头且包含 "html"，尝试激进提取
        if (trimmed.startsWith("{") && trimmed.contains("\"html\"")) {
            String extracted = aggressiveExtract(trimmed);
            if (extracted != null) {
                log.warn("使用激进提取移除JSON外壳");
                return extracted;
            }
        }

        return trimmed;
    }
}