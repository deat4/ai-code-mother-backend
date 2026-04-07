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
     * 支持单个 JSON、嵌套 JSON、多个 JSON 拼接的情况
     */
    private String extractFromJson(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试直接解析单个 JSON
            JSONObject json = new JSONObject(content);
            return extractHtmlFromJsonObject(json);
        } catch (Exception e1) {
            // 如果失败，尝试处理多个 JSON 对象拼接的情况
            // 例如: { "html": "第一部分" }\n{ "html": "第二部分" }
            try {
                // 找到所有 JSON 对象
                StringBuilder htmlBuilder = new StringBuilder();
                int start = 0;
                while (start < content.length()) {
                    int objStart = content.indexOf('{', start);
                    if (objStart == -1) break;
                    
                    // 找到匹配的 }
                    int braceCount = 0;
                    int objEnd = -1;
                    for (int i = objStart; i < content.length(); i++) {
                        if (content.charAt(i) == '{') braceCount++;
                        if (content.charAt(i) == '}') braceCount--;
                        if (braceCount == 0) {
                            objEnd = i;
                            break;
                        }
                    }
                    
                    if (objEnd == -1) break;
                    
                    // 提取并解析这个 JSON 对象
                    String jsonStr = content.substring(objStart, objEnd + 1);
                    try {
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        String html = extractHtmlFromJsonObject(jsonObj);
                        if (html != null && !html.trim().isEmpty()) {
                            htmlBuilder.append(html);
                        }
                    } catch (Exception ignored) {
                        // 忽略解析失败的 JSON
                    }
                    
                    start = objEnd + 1;
                }
                
                if (htmlBuilder.length() > 0) {
                    return htmlBuilder.toString();
                }
            } catch (Exception e2) {
                // 忽略
            }
            
            return null;
        }
    }
    
    /**
     * 从 JSON 对象中提取 HTML
     */
    private String extractHtmlFromJsonObject(JSONObject json) {
        // 1. 尝试 "htmlCode" 字段（LangChain4j 序列化格式）
        String htmlCode = json.getStr("htmlCode");
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            // 检查是否是嵌套 JSON
            if (htmlCode.trim().startsWith("{")) {
                try {
                    JSONObject nested = new JSONObject(htmlCode);
                    String nestedHtml = nested.getStr("html");
                    if (nestedHtml != null && !nestedHtml.trim().isEmpty()) {
                        return nestedHtml;
                    }
                    String nestedCode = nested.getStr("htmlCode");
                    if (nestedCode != null && !nestedCode.trim().isEmpty()) {
                        return nestedCode;
                    }
                } catch (Exception ignored) {}
            }
            return htmlCode;
        }
        
        // 2. 尝试 "html" 字段（prompt 要求的格式）
        String html = json.getStr("html");
        if (html != null && !html.trim().isEmpty()) {
            // 检查是否是嵌套 JSON
            if (html.trim().startsWith("{")) {
                try {
                    JSONObject nested = new JSONObject(html);
                    String nestedHtml = nested.getStr("html");
                    if (nestedHtml != null && !nestedHtml.trim().isEmpty()) {
                        return nestedHtml;
                    }
                    String nestedCode = nested.getStr("htmlCode");
                    if (nestedCode != null && !nestedCode.trim().isEmpty()) {
                        return nestedCode;
                    }
                } catch (Exception ignored) {}
            }
            return html;
        }
        
        return null;
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