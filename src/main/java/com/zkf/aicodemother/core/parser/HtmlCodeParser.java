package com.zkf.aicodemother.core.parser;

import cn.hutool.json.JSONObject;
import com.zkf.aicodemother.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 *
 * @author yupi
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    // 优化后的正则：兼容 ```html 后直接跟内容，或带有不同空白符的情况
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            return new HtmlCodeResult();
        }

        HtmlCodeResult result = new HtmlCodeResult();

        // 调试日志：打印原始内容前100字符
        System.out.println("[DEBUG] HtmlCodeParser 原始内容前100字符: " + 
            codeContent.substring(0, Math.min(100, codeContent.length())));

        // 1. 尝试从 JSON 格式提取
        String htmlCode = extractFromJson(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            System.out.println("[DEBUG] 从 JSON 提取成功");
            result.setHtmlCode(htmlCode.trim());
            return result;
        }

        // 2. 尝试从 ```html 代码块提取
        htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            System.out.println("[DEBUG] 从代码块提取成功");
            // 对代码块内容也应用反转义
            result.setHtmlCode(unescapeJsonString(htmlCode.trim()));
            return result;
        }

        // 3. 如果都没有，将整个内容作为 HTML（应用反转义）
        System.out.println("[DEBUG] 使用原始内容作为HTML");
        result.setHtmlCode(unescapeJsonString(codeContent.trim()));
        return result;
    }

    /**
     * 从 JSON 格式提取 HTML 代码
     * 支持纯 JSON 和 Markdown 代码块包裹的 JSON
     * JSON 解析失败时尝试从残缺内容中提取 HTML 骨架
     */
    private String extractFromJson(String content) {
        try {
            // 1. 先尝试提取 Markdown 代码块中的内容
            String jsonContent = content;

            // 匹配 ```json ... ``` 或 ``` ... ```
            Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
            Matcher codeBlockMatcher = codeBlockPattern.matcher(content.trim());
            if (codeBlockMatcher.find()) {
                jsonContent = codeBlockMatcher.group(1).trim();
            }

            // 2. 解析 JSON
            JSONObject json = new JSONObject(jsonContent);
            String htmlCode = json.getStr("html");
            
            // 3. 处理 JSON 字符串转义：\n -> 实际换行符
            if (htmlCode != null) {
                System.out.println("[DEBUG] JSON解析后 html 前50字符: " + 
                    htmlCode.substring(0, Math.min(50, htmlCode.length())));
                System.out.println("[DEBUG] 包含字面量\\n: " + htmlCode.contains("\\n"));
                System.out.println("[DEBUG] 包含实际换行符: " + htmlCode.contains("\n"));
                htmlCode = unescapeJsonString(htmlCode);
                System.out.println("[DEBUG] unescape后 html 前50字符: " + 
                    htmlCode.substring(0, Math.min(50, htmlCode.length())));
            }
            return htmlCode;
        } catch (Exception e) {
            // 4. JSON 解析失败时，尝试从残缺内容中提取 HTML 骨架
            return extractHtmlSkeleton(content);
        }
    }

    /**
     * 反转义 JSON 字符串中的转义序列
     * 将 \n, \t, \r, \", \\ 等转换为实际字符
     * 注意：处理顺序很重要，先处理特殊转义，最后处理 \\
     */
    private String unescapeJsonString(String str) {
        if (str == null) return null;
        // 使用 StringBuilder 逐字符处理，更可靠
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n' -> { result.append('\n'); i++; }
                    case 't' -> { result.append('\t'); i++; }
                    case 'r' -> { result.append('\r'); i++; }
                    case '"' -> { result.append('"'); i++; }
                    case '\\' -> { result.append('\\'); i++; }
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 从残缺的 JSON 或破损内容中提取 HTML 骨架
     * 处理 AI 输出被截断导致 JSON 不完整的情况
     */
    private String extractHtmlSkeleton(String content) {
        // 尝试匹配 "html": "... 后面的内容
        // 这种情况是 JSON 被截断，但 HTML 内容部分存在
        Pattern htmlValuePattern = Pattern.compile("\"html\"\\s*:\\s*\"([\\s\\S]*)");
        Matcher htmlValueMatcher = htmlValuePattern.matcher(content);
        if (htmlValueMatcher.find()) {
            String htmlContent = htmlValueMatcher.group(1);
            // 移除末尾可能存在的 JSON 结束符或转义字符
            // 保留 HTML 内容直到找到完整的结构
            return unescapeJsonString(cleanHtmlContent(htmlContent));
        }

        // 尝试匹配 <!DOCTYPE html> 或 <html> 开始的内容
        Pattern doctypePattern = Pattern.compile("(<!DOCTYPE html[\\s\\S]*)", Pattern.CASE_INSENSITIVE);
        Matcher doctypeMatcher = doctypePattern.matcher(content);
        if (doctypeMatcher.find()) {
            return unescapeJsonString(cleanHtmlContent(doctypeMatcher.group(1)));
        }

        Pattern htmlTagPattern = Pattern.compile("(<html[\\s\\S]*)", Pattern.CASE_INSENSITIVE);
        Matcher htmlTagMatcher = htmlTagPattern.matcher(content);
        if (htmlTagMatcher.find()) {
            return unescapeJsonString(cleanHtmlContent(htmlTagMatcher.group(1)));
        }

        return null;
    }

    /**
     * 清理 HTML 内容，移除末尾的 JSON 残留
     */
    private String cleanHtmlContent(String content) {
        if (content == null) return null;

        // 移除末尾的 JSON 结束符和转义字符
        // 找到最后一个完整的 HTML 标签结束
        int lastHtmlEnd = content.lastIndexOf("</html>");
        if (lastHtmlEnd > 0) {
            return content.substring(0, lastHtmlEnd + 7); // 包含 </html>
        }

        lastHtmlEnd = content.lastIndexOf("</body>");
        if (lastHtmlEnd > 0) {
            return content.substring(0, lastHtmlEnd + 7);
        }

        // 如果没有找到完整的结束标签，尝试移除末尾的 JSON 残留
        // 常见模式: "}", }", \", etc.
        content = content.replaceAll("[\\\\]?\"\\s*\\}?\\s*$", "");

        return content;
    }

    /**
     * 提取 HTML 代码块内容
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
