package com.zkf.aicodemother.core.validation;

import cn.hutool.core.io.FileUtil;
import com.zkf.aicodemother.core.validation.impl.HtmlValidationService;
import com.zkf.aicodemother.core.validation.impl.MultiFileValidationService;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 校验语义规则手动测试
 * 用于验证 passed / issueCount / warningCount 的语义统一
 */
public class ValidationManualTest {

    public static void main(String[] args) {
        System.out.println("=== 校验语义规则测试 ===\n");

        HtmlValidationService htmlService = new HtmlValidationService();
        MultiFileValidationService multiFileService = new MultiFileValidationService();

        String basePath = "E:/BankProject/ai-code-mother/ai-code-mother/testcases/";

        // 测试 1: 正常 HTML（应通过，无警告）
        testHtmlPass(htmlService, basePath + "html/pass/index.html");

        // 测试 2: 有警告的 HTML（应通过，有警告）
        testHtmlWithWarning(htmlService, basePath + "html/pass/index_with_warning.html");

        // 测试 3: 失败的 HTML（缺少文件）
        testHtmlFail(htmlService, basePath + "html/fail");

        // 测试语义规则
        System.out.println("\n=== 语义规则验证 ===");
        verifySemanticRules();
    }

    private static void testHtmlPass(HtmlValidationService service, String filePath) {
        System.out.println("测试 1: 正常 HTML 文件");
        System.out.println("文件: " + filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("文件不存在，跳过");
            return;
        }

        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        System.out.println("内容长度: " + content.length() + " 字节");

        // 创建测试上下文（使用临时 appId）
        ValidationContext context = ValidationContext.builder()
                .appId(999001L)
                .codeGenType("HTML")
                .projectRootPath(file.getParent())
                .build();

        ValidationResult result = service.validateHtml(context);

        printResult(result);
        System.out.println();
    }

    private static void testHtmlWithWarning(HtmlValidationService service, String filePath) {
        System.out.println("测试 2: 有警告的 HTML 文件（残留 markdown）");
        System.out.println("文件: " + filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("文件不存在，跳过");
            return;
        }

        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        boolean hasMarkdown = content.contains("```");
        System.out.println("包含 markdown 标记: " + hasMarkdown);

        ValidationContext context = ValidationContext.builder()
                .appId(999002L)
                .codeGenType("HTML")
                .projectRootPath(file.getParent())
                .build();

        ValidationResult result = service.validateHtml(context);

        printResult(result);

        // 验证语义：有警告但 passed=true
        if (result.getWarningCount() > 0 && result.isPassedByErrors()) {
            System.out.println("✅ 语义正确：有警告但 passed=true");
        } else {
            System.out.println("❌ 语义错误：警告不应影响 passed");
        }
        System.out.println();
    }

    private static void testHtmlFail(HtmlValidationService service, String dirPath) {
        System.out.println("测试 3: 失败的 HTML 目录（无 index.html）");
        System.out.println("目录: " + dirPath);

        File dir = new File(dirPath);
        File indexFile = new File(dir, "index.html");
        System.out.println("index.html 存在: " + indexFile.exists());

        ValidationContext context = ValidationContext.builder()
                .appId(999003L)
                .codeGenType("HTML")
                .projectRootPath(dirPath)
                .build();

        ValidationResult result = service.validateHtml(context);

        printResult(result);

        // 验证语义：passed=false, issueCount>=1
        if (!result.isPassedByErrors() && result.getErrorCount() >= 1) {
            System.out.println("✅ 语义正确：有 ERROR 时 passed=false");
        } else {
            System.out.println("❌ 语义错误");
        }
        System.out.println();
    }

    private static void printResult(ValidationResult result) {
        System.out.println("--- 校验结果 ---");
        System.out.println("passed: " + result.isPassedByErrors());
        System.out.println("summary: " + result.getSummary());
        System.out.println("ERROR 数量 (issueCount): " + result.getErrorCount());
        System.out.println("WARN 数量 (warningCount): " + result.getWarningCount());

        if (!result.getIssues().isEmpty()) {
            System.out.println("issues:");
            for (ValidationIssue issue : result.getIssues()) {
                System.out.printf("  - [%s] %s: %s%n",
                        issue.getSeverity().toUpperCase(),
                        issue.getRuleCode(),
                        issue.getMessage());
            }
        }
    }

    private static void verifySemanticRules() {
        System.out.println("验证核心语义规则:");

        // 规则 1: passed 只看 ERROR
        ValidationResult r1 = ValidationResult.builder()
                .issues(new java.util.ArrayList<>())
                .build();
        r1.addIssue(ValidationIssue.warn("content", "WARN", "警告"));
        boolean passedWithWarn = r1.isPassedByErrors();
        System.out.println("规则 1 [passed 只看 ERROR]: " +
                (passedWithWarn ? "✅ 有 WARN 时 passed=true" : "❌"));

        // 规则 2: issueCount 只统计 ERROR
        ValidationResult r2 = ValidationResult.builder()
                .issues(new java.util.ArrayList<>())
                .build();
        r2.addIssue(ValidationIssue.warn("content", "W1", "警告1"));
        r2.addIssue(ValidationIssue.warn("content", "W2", "警告2"));
        r2.addIssue(ValidationIssue.error("structure", "E1", "错误1"));
        int issueCount = r2.getErrorCount();
        System.out.println("规则 2 [issueCount 只统计 ERROR]: " +
                (issueCount == 1 ? "✅ issueCount=1 (不是3)" : "❌ issueCount=" + issueCount));

        // 规则 3: warningCount 只统计 WARN
        int warningCount = r2.getWarningCount();
        System.out.println("规则 3 [warningCount 只统计 WARN]: " +
                (warningCount == 2 ? "✅ warningCount=2" : "❌ warningCount=" + warningCount));

        // 规则 4: severity 格式
        ValidationIssue error = ValidationIssue.error("test", "CODE", "测试");
        ValidationIssue warn = ValidationIssue.warn("test", "CODE", "测试");
        System.out.println("规则 4 [severity 小写格式]: " +
                ("error".equals(error.getSeverity()) && "warn".equals(warn.getSeverity())
                        ? "✅ severity='error'/'warn'" : "❌"));
    }
}