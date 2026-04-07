package com.zkf.aicodemother.core.saver;

import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代码文件保存器测试
 */
class CodeFileSaverTest {

    @Test
    void testSaveHtmlCode() {
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("<!DOCTYPE html><html><body><h1>Test</h1></body></html>");
        
        File savedDir = new HtmlCodeFileSaverTemplate().saveCode(result);
        
        assertNotNull(savedDir);
        assertTrue(savedDir.exists());
        assertTrue(savedDir.isDirectory());
        assertTrue(savedDir.getName().startsWith("html_"));
        
        // 验证文件是否存在
        File htmlFile = new File(savedDir, "index.html");
        assertTrue(htmlFile.exists());
        
        System.out.println("HTML 代码保存路径: " + savedDir.getAbsolutePath());
    }

    @Test
    void testSaveMultiFileCode() {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"style.css\"></head><body><h1>Test</h1></body></html>");
        result.setCssCode("h1 { color: blue; }");
        result.setJsCode("console.log('test');");
        
        File savedDir = new MultiFileCodeFileSaverTemplate().saveCode(result);
        
        assertNotNull(savedDir);
        assertTrue(savedDir.exists());
        assertTrue(savedDir.isDirectory());
        assertTrue(savedDir.getName().startsWith("multi_file_"));
        
        // 验证文件是否存在
        assertTrue(new File(savedDir, "index.html").exists());
        assertTrue(new File(savedDir, "style.css").exists());
        assertTrue(new File(savedDir, "script.js").exists());
        
        System.out.println("多文件代码保存路径: " + savedDir.getAbsolutePath());
    }

    @Test
    void testSaveHtmlCodeWithNullResult() {
        HtmlCodeFileSaverTemplate saver = new HtmlCodeFileSaverTemplate();
        assertThrows(Exception.class, () -> saver.saveCode(null));
    }

    @Test
    void testSaveHtmlCodeWithEmptyCode() {
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("");
        
        HtmlCodeFileSaverTemplate saver = new HtmlCodeFileSaverTemplate();
        assertThrows(Exception.class, () -> saver.saveCode(result));
    }
}