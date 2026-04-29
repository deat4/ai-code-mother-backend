package com.zkf.aicodemother.core.validation;

/**
 * 语义规则纯 Java 测试（不依赖 Spring）
 * 直接运行 main 方法验证 passed / issueCount / warningCount 语义
 */
public class SemanticRulePureTest {

    public static void main(String[] args) {
        System.out.println("=== 校验语义规则验证 ===\n");

        // 测试 1: 只有警告
        testOnlyWarnings();

        // 测试 2: 有错误和警告
        testErrorsAndWarnings();

        // 测试 3: 无问题
        testNoIssues();

        // 测试 4: severity 格式
        testSeverityFormat();

        System.out.println("\n=== 所有语义规则测试完成 ===");
    }

    static void testOnlyWarnings() {
        System.out.println("测试 1: 只有警告（应通过）");

        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        result.addIssue(ValidationIssue.warn("content", "MARKDOWN_REMAINED", "残留 markdown"));
        result.addIssue(ValidationIssue.warn("content", "PLACEHOLDER_FOUND", "存在占位符"));

        System.out.println("  WARN 数量: " + result.getWarningCount());
        System.out.println("  ERROR 数量: " + result.getErrorCount());
        System.out.println("  passed (byErrors): " + result.isPassedByErrors());

        boolean passed = result.isPassedByErrors();
        boolean warnCountCorrect = result.getWarningCount() == 2;
        boolean errorCountCorrect = result.getErrorCount() == 0;

        if (passed && warnCountCorrect && errorCountCorrect) {
            System.out.println("  ✅ 语义正确：只有警告时 passed=true, warningCount=2, issueCount=0");
        } else {
            System.out.println("  ❌ 语义错误！");
        }
        System.out.println();
    }

    static void testErrorsAndWarnings() {
        System.out.println("测试 2: 有错误和警告（应失败）");

        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        result.addIssue(ValidationIssue.warn("content", "WARN_1", "警告1"));
        result.addIssue(ValidationIssue.warn("content", "WARN_2", "警告2"));
        result.addIssue(ValidationIssue.error("structure", "INDEX_MISSING", "缺少入口文件"));
        result.addIssue(ValidationIssue.error("structure", "HTML_TAG_MISSING", "缺少html标签"));

        System.out.println("  WARN 数量: " + result.getWarningCount());
        System.out.println("  ERROR 数量 (issueCount): " + result.getErrorCount());
        System.out.println("  passed (byErrors): " + result.isPassedByErrors());

        boolean failed = !result.isPassedByErrors();
        boolean warnCountCorrect = result.getWarningCount() == 2;
        boolean errorCountCorrect = result.getErrorCount() == 2;

        if (failed && warnCountCorrect && errorCountCorrect) {
            System.out.println("  ✅ 语义正确：有 ERROR 时 passed=false, issueCount=2, warningCount=2");
        } else {
            System.out.println("  ❌ 语义错误！");
        }
        System.out.println();
    }

    static void testNoIssues() {
        System.out.println("测试 3: 无任何问题（应通过）");

        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        System.out.println("  WARN 数量: " + result.getWarningCount());
        System.out.println("  ERROR 数量: " + result.getErrorCount());
        System.out.println("  passed (byErrors): " + result.isPassedByErrors());

        if (result.isPassedByErrors() && result.getErrorCount() == 0 && result.getWarningCount() == 0) {
            System.out.println("  ✅ 语义正确：无问题时 passed=true, issueCount=0, warningCount=0");
        } else {
            System.out.println("  ❌ 语义错误！");
        }
        System.out.println();
    }

    static void testSeverityFormat() {
        System.out.println("测试 4: severity 格式（应为小写）");

        ValidationIssue error = ValidationIssue.error("structure", "TEST", "测试错误");
        ValidationIssue warn = ValidationIssue.warn("content", "TEST", "测试警告");

        System.out.println("  error severity: '" + error.getSeverity() + "'");
        System.out.println("  warn severity: '" + warn.getSeverity() + "'");

        boolean errorCorrect = "error".equals(error.getSeverity());
        boolean warnCorrect = "warn".equals(warn.getSeverity());

        if (errorCorrect && warnCorrect) {
            System.out.println("  ✅ severity 格式正确：'error' 和 'warn'（小写）");
        } else {
            System.out.println("  ❌ severity 格式错误！");
        }
        System.out.println();
    }
}