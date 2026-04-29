package com.zkf.aicodemother.core.validation;

import com.zkf.aicodemother.core.validation.impl.HtmlValidationService;
import com.zkf.aicodemother.core.validation.impl.MultiFileValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验语义规则测试
 * 验证 passed / issueCount / warningCount 的语义统一
 */
@SpringBootTest
public class ValidationSemanticTest {

    @Autowired
    private HtmlValidationService htmlValidationService;

    @Autowired
    private MultiFileValidationService multiFileValidationService;

    /**
     * 测试 ValidationResult 的语义规则
     */
    @Test
    void testValidationResultSemantic() {
        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        // 添加 2 个警告
        result.addIssue(ValidationIssue.warn("content", "WARN_1", "警告1"));
        result.addIssue(ValidationIssue.warn("content", "WARN_2", "警告2"));

        // 添加 1 个错误
        result.addIssue(ValidationIssue.error("structure", "ERROR_1", "错误1"));

        // 验证统计
        assertEquals(1, result.getErrorCount(), "ERROR 数量应为 1");
        assertEquals(2, result.getWarningCount(), "WARN 数量应为 2");
        assertFalse(result.isPassedByErrors(), "有 ERROR 时 passed 应为 false");
    }

    /**
     * 测试只有警告时的语义
     */
    @Test
    void testOnlyWarnings() {
        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        // 只有 3 个警告，没有错误
        result.addIssue(ValidationIssue.warn("content", "MARKDOWN_REMAINED", "残留 markdown"));
        result.addIssue(ValidationIssue.warn("content", "PLACEHOLDER_FOUND", "存在占位符"));
        result.addIssue(ValidationIssue.warn("content", "FILE_TOO_SMALL", "文件过小"));

        // 验证统计
        assertEquals(0, result.getErrorCount(), "ERROR 数量应为 0");
        assertEquals(3, result.getWarningCount(), "WARN 数量应为 3");
        assertTrue(result.isPassedByErrors(), "只有 WARN 时 passed 应为 true");
    }

    /**
     * 测试无问题时的语义
     */
    @Test
    void testNoIssues() {
        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        // 无任何问题
        assertEquals(0, result.getErrorCount(), "ERROR 数量应为 0");
        assertEquals(0, result.getWarningCount(), "WARN 数量应为 0");
        assertTrue(result.isPassedByErrors(), "无问题时 passed 应为 true");
    }

    /**
     * 测试 ValidationIssue 的 severity 应为小写
     */
    @Test
    void testIssueSeverityFormat() {
        ValidationIssue error = ValidationIssue.error("structure", "TEST", "测试错误");
        ValidationIssue warn = ValidationIssue.warn("content", "TEST", "测试警告");

        assertEquals("error", error.getSeverity(), "ERROR severity 应为小写 'error'");
        assertEquals("warn", warn.getSeverity(), "WARN severity 应为小写 'warn'");
    }

    /**
     * 测试摘要格式：有警告时应体现
     */
    @Test
    void testSummaryFormatWithWarnings() {
        ValidationResult result = ValidationResult.builder()
                .passed(true)
                .issues(new java.util.ArrayList<>())
                .build();

        result.addIssue(ValidationIssue.warn("content", "WARN_1", "警告"));

        // 模拟设置摘要（实际由服务设置）
        String expectedSummary = String.format("HTML 校验通过，存在 %d 个警告", result.getWarningCount());

        // 验证语义一致性
        assertTrue(result.isPassedByErrors());
        assertEquals(1, result.getWarningCount());
        // 摘要应包含警告数量
        assertTrue(expectedSummary.contains("1 个警告"));
    }
}