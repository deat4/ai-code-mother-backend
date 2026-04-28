package com.zkf.aicodemother.core.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验问题
 * 表示单个校验失败点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationIssue {

    /**
     * 问题类型：STRUCTURE / CONTENT / BUILD / CONFIG
     */
    private String type;

    /**
     * 严重性：INFO / WARN / ERROR
     */
    private String severity;

    /**
     * 相关文件路径，可为空
     */
    private String filePath;

    /**
     * 规则编码，便于后续修复和统计
     * 例如：INDEX_MISSING、MARKDOWN_REMAINED、BUILD_FAILED
     */
    private String ruleCode;

    /**
     * 面向开发者的错误描述
     */
    private String message;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 创建错误级别问题
     */
    public static ValidationIssue error(String type, String ruleCode, String message) {
        return ValidationIssue.builder()
                .type(type)
                .severity("error")
                .ruleCode(ruleCode)
                .message(message)
                .build();
    }

    /**
     * 创建错误级别问题（带文件路径）
     */
    public static ValidationIssue error(String type, String filePath, String ruleCode, String message) {
        return ValidationIssue.builder()
                .type(type)
                .severity("error")
                .filePath(filePath)
                .ruleCode(ruleCode)
                .message(message)
                .build();
    }

    /**
     * 创建警告级别问题
     */
    public static ValidationIssue warn(String type, String ruleCode, String message) {
        return ValidationIssue.builder()
                .type(type)
                .severity("warn")
                .ruleCode(ruleCode)
                .message(message)
                .build();
    }

    /**
     * 创建警告级别问题（带文件路径）
     */
    public static ValidationIssue warn(String type, String filePath, String ruleCode, String message) {
        return ValidationIssue.builder()
                .type(type)
                .severity("warn")
                .filePath(filePath)
                .ruleCode(ruleCode)
                .message(message)
                .build();
    }
}