package com.zkf.aicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;

/**
 * 多文件代码保存器
 *
 * @author yupi
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        System.out.println("[DEBUG] saveFiles - htmlCode: " + (result.getHtmlCode() != null ? result.getHtmlCode().length() : "null"));
        System.out.println("[DEBUG] saveFiles - cssCode: " + (result.getCssCode() != null ? result.getCssCode().length() : "null"));
        System.out.println("[DEBUG] saveFiles - jsCode: " + (result.getJsCode() != null ? result.getJsCode().length() : "null"));
        
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        
        // 保存 CSS 文件（使用复数形式匹配 HTML 引用）
        // 如果 CSS 为空或只有占位符，生成有用的默认样式
        String cssCode = result.getCssCode();
        String cssToWrite;
        if (StrUtil.isBlank(cssCode) || cssCode.trim().equals("/* 样式文件 */") || cssCode.trim().length() < 20) {
            cssToWrite = generateDefaultCss(result.getHtmlCode());
            System.out.println("[DEBUG] CSS 内容无效，生成默认样式，长度: " + cssToWrite.length());
        } else {
            cssToWrite = cssCode;
        }
        writeToFile(baseDirPath, "styles.css", cssToWrite);
        
        // 保存 JavaScript 文件
        // 如果 JS 为空或只有占位符，生成有用的默认脚本
        String jsCode = result.getJsCode();
        String jsToWrite;
        if (StrUtil.isBlank(jsCode) || jsCode.trim().equals("// 脚本文件") || jsCode.trim().length() < 20) {
            jsToWrite = generateDefaultJs(result.getHtmlCode());
            System.out.println("[DEBUG] JS 内容无效，生成默认脚本，长度: " + jsToWrite.length());
        } else {
            jsToWrite = jsCode;
        }
        writeToFile(baseDirPath, "script.js", jsToWrite);
    }
    
    /**
     * 根据 HTML 内容生成默认 CSS 样式
     */
    private String generateDefaultCss(String htmlCode) {
        StringBuilder css = new StringBuilder();
        
        // 基础样式重置
        css.append("/* ========== 基础样式重置 ========== */\n");
        css.append("* {\n");
        css.append("    margin: 0;\n");
        css.append("    padding: 0;\n");
        css.append("    box-sizing: border-box;\n");
        css.append("}\n\n");
        
        // 根元素样式
        css.append("html {\n");
        css.append("    font-size: 16px;\n");
        css.append("    scroll-behavior: smooth;\n");
        css.append("}\n\n");
        
        css.append("body {\n");
        css.append("    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\n");
        css.append("    line-height: 1.6;\n");
        css.append("    color: #333;\n");
        css.append("    background-color: #f8f9fa;\n");
        css.append("    min-height: 100vh;\n");
        css.append("}\n\n");
        
        // 如果 HTML 包含特定元素，添加对应样式
        if (htmlCode != null) {
            String lowerHtml = htmlCode.toLowerCase();
            
            // 容器样式
            if (lowerHtml.contains("class=\"container") || lowerHtml.contains("class='container")) {
                css.append("/* ========== 容器布局 ========== */\n");
                css.append(".container {\n");
                css.append("    max-width: 1200px;\n");
                css.append("    margin: 0 auto;\n");
                css.append("    padding: 20px;\n");
                css.append("}\n\n");
            }
            
            // 标题样式
            if (lowerHtml.contains("<h1") || lowerHtml.contains("<h2") || lowerHtml.contains("<h3")) {
                css.append("/* ========== 标题样式 ========== */\n");
                css.append("h1, h2, h3, h4, h5, h6 {\n");
                css.append("    font-weight: 600;\n");
                css.append("    line-height: 1.3;\n");
                css.append("    margin-bottom: 0.5em;\n");
                css.append("    color: #1a1a2e;\n");
                css.append("}\n\n");
                css.append("h1 { font-size: 2.5rem; }\n");
                css.append("h2 { font-size: 2rem; }\n");
                css.append("h3 { font-size: 1.5rem; }\n");
                css.append("h4 { font-size: 1.25rem; }\n\n");
            }
            
            // 段落样式
            if (lowerHtml.contains("<p")) {
                css.append("/* ========== 段落样式 ========== */\n");
                css.append("p {\n");
                css.append("    margin-bottom: 1rem;\n");
                css.append("    color: #555;\n");
                css.append("}\n\n");
            }
            
            // 链接样式
            if (lowerHtml.contains("<a ")) {
                css.append("/* ========== 链接样式 ========== */\n");
                css.append("a {\n");
                css.append("    color: #4361ee;\n");
                css.append("    text-decoration: none;\n");
                css.append("    transition: color 0.3s ease, transform 0.2s ease;\n");
                css.append("}\n\n");
                css.append("a:hover {\n");
                css.append("    color: #3a0ca3;\n");
                css.append("    text-decoration: underline;\n");
                css.append("}\n\n");
            }
            
            // 按钮样式
            if (lowerHtml.contains("<button") || lowerHtml.contains("class=\"btn") || lowerHtml.contains("class='btn")) {
                css.append("/* ========== 按钮样式 ========== */\n");
                css.append("button, .btn {\n");
                css.append("    display: inline-block;\n");
                css.append("    padding: 12px 24px;\n");
                css.append("    font-size: 1rem;\n");
                css.append("    font-weight: 500;\n");
                css.append("    color: #fff;\n");
                css.append("    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n");
                css.append("    border: none;\n");
                css.append("    border-radius: 8px;\n");
                css.append("    cursor: pointer;\n");
                css.append("    transition: all 0.3s ease;\n");
                css.append("    box-shadow: 0 4px 6px rgba(102, 126, 234, 0.3);\n");
                css.append("}\n\n");
                css.append("button:hover, .btn:hover {\n");
                css.append("    transform: translateY(-2px);\n");
                css.append("    box-shadow: 0 6px 12px rgba(102, 126, 234, 0.4);\n");
                css.append("}\n\n");
                css.append("button:active, .btn:active {\n");
                css.append("    transform: translateY(0);\n");
                css.append("}\n\n");
            }
            
            // 输入框样式
            if (lowerHtml.contains("<input") || lowerHtml.contains("<textarea") || lowerHtml.contains("<select")) {
                css.append("/* ========== 表单样式 ========== */\n");
                css.append("input, textarea, select {\n");
                css.append("    width: 100%;\n");
                css.append("    padding: 12px 16px;\n");
                css.append("    font-size: 1rem;\n");
                css.append("    border: 2px solid #e0e0e0;\n");
                css.append("    border-radius: 8px;\n");
                css.append("    background-color: #fff;\n");
                css.append("    transition: border-color 0.3s ease, box-shadow 0.3s ease;\n");
                css.append("}\n\n");
                css.append("input:focus, textarea:focus, select:focus {\n");
                css.append("    outline: none;\n");
                css.append("    border-color: #667eea;\n");
                css.append("    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);\n");
                css.append("}\n\n");
                css.append("label {\n");
                css.append("    display: block;\n");
                css.append("    margin-bottom: 8px;\n");
                css.append("    font-weight: 500;\n");
                css.append("    color: #333;\n");
                css.append("}\n\n");
            }
            
            // 卡片样式
            if (lowerHtml.contains("class=\"card") || lowerHtml.contains("class='card")) {
                css.append("/* ========== 卡片样式 ========== */\n");
                css.append(".card {\n");
                css.append("    background: #fff;\n");
                css.append("    border-radius: 12px;\n");
                css.append("    padding: 24px;\n");
                css.append("    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);\n");
                css.append("    transition: transform 0.3s ease, box-shadow 0.3s ease;\n");
                css.append("}\n\n");
                css.append(".card:hover {\n");
                css.append("    transform: translateY(-4px);\n");
                css.append("    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);\n");
                css.append("}\n\n");
            }
            
            // 导航栏样式
            if (lowerHtml.contains("<nav") || lowerHtml.contains("class=\"nav") || lowerHtml.contains("class='nav")) {
                css.append("/* ========== 导航栏样式 ========== */\n");
                css.append("nav, .nav {\n");
                css.append("    display: flex;\n");
                css.append("    align-items: center;\n");
                css.append("    justify-content: space-between;\n");
                css.append("    padding: 16px 24px;\n");
                css.append("    background: #fff;\n");
                css.append("    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);\n");
                css.append("    position: sticky;\n");
                css.append("    top: 0;\n");
                css.append("    z-index: 1000;\n");
                css.append("}\n\n");
            }
            
            // 图片样式
            if (lowerHtml.contains("<img")) {
                css.append("/* ========== 图片样式 ========== */\n");
                css.append("img {\n");
                css.append("    max-width: 100%;\n");
                css.append("    height: auto;\n");
                css.append("    border-radius: 8px;\n");
                css.append("}\n\n");
            }
            
            // 列表样式
            if (lowerHtml.contains("<ul") || lowerHtml.contains("<ol")) {
                css.append("/* ========== 列表样式 ========== */\n");
                css.append("ul, ol {\n");
                css.append("    margin-left: 1.5rem;\n");
                css.append("    margin-bottom: 1rem;\n");
                css.append("}\n\n");
                css.append("li {\n");
                css.append("    margin-bottom: 0.5rem;\n");
                css.append("}\n\n");
            }
            
            // 页脚样式
            if (lowerHtml.contains("<footer")) {
                css.append("/* ========== 页脚样式 ========== */\n");
                css.append("footer {\n");
                css.append("    margin-top: auto;\n");
                css.append("    padding: 24px;\n");
                css.append("    background: #1a1a2e;\n");
                css.append("    color: #fff;\n");
                css.append("    text-align: center;\n");
                css.append("}\n\n");
            }
            
            // Grid 布局
            if (lowerHtml.contains("class=\"grid") || lowerHtml.contains("class='grid")) {
                css.append("/* ========== Grid 布局 ========== */\n");
                css.append(".grid {\n");
                css.append("    display: grid;\n");
                css.append("    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));\n");
                css.append("    gap: 24px;\n");
                css.append("}\n\n");
            }
            
            // Flex 布局
            if (lowerHtml.contains("class=\"flex") || lowerHtml.contains("class='flex")) {
                css.append("/* ========== Flex 布局 ========== */\n");
                css.append(".flex {\n");
                css.append("    display: flex;\n");
                css.append("    flex-wrap: wrap;\n");
                css.append("    gap: 16px;\n");
                css.append("}\n\n");
            }
        }
        
        // 通用工具类
        css.append("/* ========== 工具类 ========== */\n");
        css.append(".text-center { text-align: center; }\n");
        css.append(".text-left { text-align: left; }\n");
        css.append(".text-right { text-align: right; }\n");
        css.append(".mt-1 { margin-top: 0.5rem; }\n");
        css.append(".mt-2 { margin-top: 1rem; }\n");
        css.append(".mt-3 { margin-top: 1.5rem; }\n");
        css.append(".mb-1 { margin-bottom: 0.5rem; }\n");
        css.append(".mb-2 { margin-bottom: 1rem; }\n");
        css.append(".mb-3 { margin-bottom: 1.5rem; }\n");
        css.append(".p-1 { padding: 0.5rem; }\n");
        css.append(".p-2 { padding: 1rem; }\n");
        css.append(".p-3 { padding: 1.5rem; }\n");
        
        return css.toString();
    }
    
    /**
     * 根据 HTML 内容生成默认 JavaScript
     */
    private String generateDefaultJs(String htmlCode) {
        StringBuilder js = new StringBuilder();
        
        js.append("// ========== 页面交互脚本 ==========\n");
        js.append("\n");
        js.append("document.addEventListener('DOMContentLoaded', function() {\n");
        js.append("    console.log('页面加载完成');\n");
        js.append("    \n");
        
        if (htmlCode != null) {
            String lowerHtml = htmlCode.toLowerCase();
            
            // 表单验证
            if (lowerHtml.contains("<form") || lowerHtml.contains("<input")) {
                js.append("    // 表单验证\n");
                js.append("    const forms = document.querySelectorAll('form');\n");
                js.append("    forms.forEach(form => {\n");
                js.append("        form.addEventListener('submit', function(e) {\n");
                js.append("            const inputs = form.querySelectorAll('input[required], textarea[required]');\n");
                js.append("            let isValid = true;\n");
                js.append("            inputs.forEach(input => {\n");
                js.append("                if (!input.value.trim()) {\n");
                js.append("                    isValid = false;\n");
                js.append("                    input.style.borderColor = '#e74c3c';\n");
                js.append("                } else {\n");
                js.append("                    input.style.borderColor = '#2ecc71';\n");
                js.append("                }\n");
                js.append("            });\n");
                js.append("            if (!isValid) {\n");
                js.append("                e.preventDefault();\n");
                js.append("                alert('请填写所有必填项');\n");
                js.append("            }\n");
                js.append("        });\n");
                js.append("    });\n");
                js.append("    \n");
            }
            
            // 平滑滚动
            if (lowerHtml.contains("href=\"#")) {
                js.append("    // 平滑滚动\n");
                js.append("    document.querySelectorAll('a[href^=\"#\"]').forEach(anchor => {\n");
                js.append("        anchor.addEventListener('click', function(e) {\n");
                js.append("            e.preventDefault();\n");
                js.append("            const target = document.querySelector(this.getAttribute('href'));\n");
                js.append("            if (target) {\n");
                js.append("                target.scrollIntoView({ behavior: 'smooth' });\n");
                js.append("            }\n");
                js.append("        });\n");
                js.append("    });\n");
                js.append("    \n");
            }
            
            // 移动端导航切换
            if (lowerHtml.contains("<nav") || lowerHtml.contains("class=\"nav")) {
                js.append("    // 移动端导航菜单切换\n");
                js.append("    const navToggle = document.querySelector('.nav-toggle');\n");
                js.append("    const navMenu = document.querySelector('.nav-menu');\n");
                js.append("    if (navToggle && navMenu) {\n");
                js.append("        navToggle.addEventListener('click', function() {\n");
                js.append("            navMenu.classList.toggle('active');\n");
                js.append("        });\n");
                js.append("    }\n");
                js.append("    \n");
            }
            
            // 卡片动画
            if (lowerHtml.contains("class=\"card")) {
                js.append("    // 卡片入场动画\n");
                js.append("    const cards = document.querySelectorAll('.card');\n");
                js.append("    const observer = new IntersectionObserver((entries) => {\n");
                js.append("        entries.forEach(entry => {\n");
                js.append("            if (entry.isIntersecting) {\n");
                js.append("                entry.target.style.opacity = '1';\n");
                js.append("                entry.target.style.transform = 'translateY(0)';\n");
                js.append("            }\n");
                js.append("        });\n");
                js.append("    }, { threshold: 0.1 });\n");
                js.append("    cards.forEach(card => {\n");
                js.append("        card.style.opacity = '0';\n");
                js.append("        card.style.transform = 'translateY(20px)';\n");
                js.append("        card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';\n");
                js.append("        observer.observe(card);\n");
                js.append("    });\n");
                js.append("    \n");
            }
        }
        
        js.append("    // 在这里添加更多交互逻辑\n");
        js.append("});\n");
        
        return js.toString();
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}