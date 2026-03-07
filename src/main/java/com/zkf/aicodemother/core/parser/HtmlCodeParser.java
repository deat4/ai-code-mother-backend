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

        // 1. 尝试从 JSON 格式提取
        String htmlCode = extractFromJson(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
            return result;
        }

        // 2. 尝试从 ```html 代码块提取
        htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
            return result;
        }

        // 3. 如果都没有，将整个内容作为 HTML
        result.setHtmlCode(codeContent.trim());
        return result;
    }

    /**
     * 从 JSON 格式提取 HTML 代码
     */
    private String extractFromJson(String content) {
        try {
            // 注意：Hutool 的 JSONObject 构造器如果传入非 JSON 字符串可能会抛异常或解析失败
            JSONObject json = new JSONObject(content);
            return json.getStr("html");
        } catch (Exception e) {
            return null;
        }
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