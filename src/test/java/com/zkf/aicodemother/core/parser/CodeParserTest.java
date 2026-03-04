package com.zkf.aicodemother.core.parser;

import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 代码解析器测试
 */
class CodeParserTest {

    @Test
    void testParseHtmlCode() {
        String codeContent = """
                随便写一段描述：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```
                随便写一段描述
                """;
        HtmlCodeResult result = new HtmlCodeParser().parseCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertTrue(result.getHtmlCode().contains("<!DOCTYPE html>"));
        System.out.println("解析的 HTML 代码:\n" + result.getHtmlCode());
    }

    @Test
    void testParseHtmlCodeWithoutCodeBlock() {
        String codeContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                """;
        HtmlCodeResult result = new HtmlCodeParser().parseCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertTrue(result.getHtmlCode().contains("<!DOCTYPE html>"));
    }

    @Test
    void testParseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：
                
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```
                
                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                
                ```js
                console.log('页面加载完成');
                ```
                
                文件创建完成！
                """;
        MultiFileCodeResult result = new MultiFileCodeParser().parseCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
        assertTrue(result.getHtmlCode().contains("<!DOCTYPE html>"));
        assertTrue(result.getCssCode().contains("color: blue"));
        assertTrue(result.getJsCode().contains("console.log"));
        
        System.out.println("解析的 HTML 代码:\n" + result.getHtmlCode());
        System.out.println("\n解析的 CSS 代码:\n" + result.getCssCode());
        System.out.println("\n解析的 JS 代码:\n" + result.getJsCode());
    }

    @Test
    void testParseMultiFileCodeWithJavaScript() {
        String codeContent = """
                ```html
                <div>Hello</div>
                ```
                ```javascript
                const greeting = "Hello World";
                console.log(greeting);
                ```
                """;
        MultiFileCodeResult result = new MultiFileCodeParser().parseCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getJsCode());
        assertTrue(result.getJsCode().contains("Hello World"));
    }
}