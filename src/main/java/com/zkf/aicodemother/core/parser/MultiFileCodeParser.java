package com.zkf.aicodemother.core.parser;

import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author yupi
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    // 支持 ---文件名: filename--- 格式
    private static final Pattern FILE_BLOCK_PATTERN = Pattern.compile(
            "---\\s*文件名\\s*[:：]\\s*(\\S+)\\s*---\\s*\\n([\\s\\S]*?)(?=---\\s*文件名|$)",
            Pattern.CASE_INSENSITIVE
    );

    // 兼容旧的代码块格式
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();

        // 优先尝试 ---文件名: filename--- 格式
        boolean parsedNewFormat = parseNewFormat(codeContent, result);

        // 如果新格式没有解析到内容，尝试旧格式
        if (!parsedNewFormat) {
            parseOldFormat(codeContent, result);
        }

        // 兜底：如果只有 HTML 没有 CSS/JS，尝试从 HTML 中提取内联样式和脚本
        if (result.getHtmlCode() != null && result.getCssCode() == null && result.getJsCode() == null) {
            System.out.println("[DEBUG] 只有 HTML，尝试提取内联 CSS/JS");
            extractInlineStylesAndScripts(result);
        }

        return result;
    }

    /**
     * 从 HTML 中提取内联 <style> 和 <script> 标签内容
     */
    private void extractInlineStylesAndScripts(MultiFileCodeResult result) {
        String htmlCode = result.getHtmlCode();

        // 提取 <style> 标签内容
        Pattern stylePattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);
        Matcher styleMatcher = stylePattern.matcher(htmlCode);
        StringBuilder cssBuilder = new StringBuilder();
        while (styleMatcher.find()) {
            String cssContent = styleMatcher.group(1).trim();
            if (!cssContent.isEmpty()) {
                cssBuilder.append(cssContent).append("\n");
            }
        }
        if (cssBuilder.length() > 0) {
            result.setCssCode(cssBuilder.toString().trim());
            System.out.println("[DEBUG] 从 HTML 提取到内联 CSS，长度: " + cssBuilder.length());
        }

        // 提取 <script> 标签内容（排除外部引用的 script）
        Pattern scriptPattern = Pattern.compile("<script(?![^>]*src=)[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher scriptMatcher = scriptPattern.matcher(htmlCode);
        StringBuilder jsBuilder = new StringBuilder();
        while (scriptMatcher.find()) {
            String jsContent = scriptMatcher.group(1).trim();
            if (!jsContent.isEmpty()) {
                jsBuilder.append(jsContent).append("\n");
            }
        }
        if (jsBuilder.length() > 0) {
            result.setJsCode(jsBuilder.toString().trim());
            System.out.println("[DEBUG] 从 HTML 提取到内联 JS，长度: " + jsBuilder.length());
        }
    }

    /**
     * 解析新格式: ---文件名: filename---
     */
    private boolean parseNewFormat(String codeContent, MultiFileCodeResult result) {
        Matcher matcher = FILE_BLOCK_PATTERN.matcher(codeContent);
        boolean found = false;

        System.out.println("[DEBUG] 尝试解析新格式，内容长度: " + codeContent.length());
        System.out.println("[DEBUG] 内容预览: " + codeContent.substring(0, Math.min(500, codeContent.length())));

        while (matcher.find()) {
            String filename = matcher.group(1).trim();
            String content = matcher.group(2).trim();

            System.out.println("[DEBUG] 找到文件: " + filename + ", 内容长度: " + content.length());

            if (content.isEmpty()) continue;

            found = true;
            String lowerFilename = filename.toLowerCase();

            if (lowerFilename.endsWith(".html") || lowerFilename.contains("index")) {
                result.setHtmlCode(content);
            } else if (lowerFilename.endsWith(".css") || lowerFilename.contains("style")) {
                result.setCssCode(content);
            } else if (lowerFilename.endsWith(".js") || lowerFilename.contains("script") || lowerFilename.equals("app.js")) {
                result.setJsCode(content);
            }
        }

        System.out.println("[DEBUG] 新格式解析结果: html=" + (result.getHtmlCode() != null) 
            + ", css=" + (result.getCssCode() != null) + ", js=" + (result.getJsCode() != null));

        return found;
    }

    /**
     * 解析旧格式: ```html```, ```css```, ```js```
     */
    private void parseOldFormat(String codeContent, MultiFileCodeResult result) {
        System.out.println("[DEBUG] 尝试解析旧格式");

        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);

        System.out.println("[DEBUG] 旧格式解析: html=" + (htmlCode != null ? htmlCode.length() : "null")
            + ", css=" + (cssCode != null ? cssCode.length() : "null")
            + ", js=" + (jsCode != null ? jsCode.length() : "null"));

        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}